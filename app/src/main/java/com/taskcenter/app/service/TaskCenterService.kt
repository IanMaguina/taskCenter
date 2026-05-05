package com.taskcenter.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.taskcenter.app.MainActivity
import com.taskcenter.app.R
import com.taskcenter.app.data.database.AppDatabase
import com.taskcenter.app.data.repository.SpaceRepository
import com.taskcenter.app.data.repository.TaskRepository
import com.taskcenter.app.network.NsdHelper
import com.taskcenter.app.network.TaskCenterClient
import com.taskcenter.app.network.TaskCenterServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TaskCenterService : LifecycleService() {
    private lateinit var server: TaskCenterServer
    private lateinit var nsdHelper: NsdHelper
    private lateinit var spaceRepo: SpaceRepository
    private lateinit var taskRepo: TaskRepository

    /** Timestamp of last successful sync per spaceId */
    private val lastSync = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        val db = AppDatabase.getInstance(this)
        spaceRepo = SpaceRepository(this)
        taskRepo = TaskRepository(this)

        server = TaskCenterServer(SERVER_PORT, db.spaceDao(), db.taskDao(), db.spaceMemberDao(), lifecycleScope)
        server.start()

        nsdHelper = NsdHelper(this)
        nsdHelper.onServiceFound = { ip, port -> onPeerDiscovered(ip, port) }
        nsdHelper.startDiscovery()

        // Register own service
        nsdHelper.registerService(SERVER_PORT, "TaskCenter-${android.os.Build.MODEL}")

        startForeground(NOTIFICATION_ID, buildNotification())
        startSyncLoop()
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
        nsdHelper.tearDown()
        Log.d(TAG, "Service destroyed")
    }

    private fun onPeerDiscovered(ip: String, port: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val spaces = TaskCenterClient.getSpaces(ip, port)
                spaces.forEach { dto ->
                    val existing = spaceRepo.getSpaceById(dto.id)
                    if (existing == null) {
                        // Save discovered space with correct network info
                        spaceRepo.saveSpace(dto.toEntity().copy(ownerIp = ip, ownerPort = port))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "onPeerDiscovered error: ${e.message}")
            }
        }
    }

    private fun startSyncLoop() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                delay(SYNC_INTERVAL_MS)
                syncAll()
            }
        }
    }

    private suspend fun syncAll() {
        val spaces = spaceRepo.getAllSpacesList()
        for (space in spaces) {
            if (space.ownerIp.isEmpty()) continue  // We are the owner, skip
            try {
                val since = lastSync[space.id] ?: 0L
                val syncData = TaskCenterClient.sync(space.ownerIp, space.ownerPort, space.id, since)
                if (syncData != null) {
                    taskRepo.saveTasksFromDto(syncData.tasks)
                    spaceRepo.saveMembersFromDto(syncData.members)
                    lastSync[space.id] = syncData.timestamp
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sync error for space ${space.id}: ${e.message}")
            }
        }
    }

    fun getDeviceIp(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            String.format(
                "%d.%d.%d.%d",
                ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff
            )
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "taskcenter_service"
        val channelName = "TaskCenter Service"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_running))
            .setSmallIcon(R.drawable.ic_tasks)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "TaskCenterService"
        const val SERVER_PORT = 8765
        private const val NOTIFICATION_ID = 1001
        private const val SYNC_INTERVAL_MS = 5000L

        fun getDeviceIp(context: Context): String {
            return try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val ip = wm.connectionInfo.ipAddress
                String.format(
                    "%d.%d.%d.%d",
                    ip and 0xff, ip shr 8 and 0xff, ip shr 16 and 0xff, ip shr 24 and 0xff
                )
            } catch (e: Exception) { "127.0.0.1" }
        }
    }
}
