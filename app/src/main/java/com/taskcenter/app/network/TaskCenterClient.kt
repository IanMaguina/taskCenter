package com.taskcenter.app.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * Simple HTTP client for communicating with other TaskCenter devices on the LAN.
 */
object TaskCenterClient {
    private const val TAG = "TaskCenterClient"
    private const val TIMEOUT_MS = 5000

    /** GET /spaces on a remote device */
    suspend fun getSpaces(ip: String, port: Int): List<SpaceDto> = withContext(Dispatchers.IO) {
        try {
            val response = get(ip, port, "/spaces")
            val type = object : com.google.gson.reflect.TypeToken<ApiResponse<List<SpaceDto>>>() {}.type
            val parsed: ApiResponse<List<SpaceDto>> = gson.fromJson(response, type)
            parsed.data ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "getSpaces $ip:$port failed: ${e.message}")
            emptyList()
        }
    }

    /** GET /spaces/{id}/tasks on a remote device */
    suspend fun getTasks(ip: String, port: Int, spaceId: String): List<TaskDto> = withContext(Dispatchers.IO) {
        try {
            val response = get(ip, port, "/spaces/$spaceId/tasks")
            val type = object : com.google.gson.reflect.TypeToken<ApiResponse<List<TaskDto>>>() {}.type
            val parsed: ApiResponse<List<TaskDto>> = gson.fromJson(response, type)
            parsed.data ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "getTasks $ip:$port/$spaceId failed: ${e.message}")
            emptyList()
        }
    }

    /** GET /spaces/{id}/members on a remote device */
    suspend fun getMembers(ip: String, port: Int, spaceId: String): List<MemberDto> = withContext(Dispatchers.IO) {
        try {
            val response = get(ip, port, "/spaces/$spaceId/members")
            val type = object : com.google.gson.reflect.TypeToken<ApiResponse<List<MemberDto>>>() {}.type
            val parsed: ApiResponse<List<MemberDto>> = gson.fromJson(response, type)
            parsed.data ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "getMembers failed: ${e.message}")
            emptyList()
        }
    }

    /** POST /spaces/{id}/tasks  – push a new task to the owner device */
    suspend fun postTask(ip: String, port: Int, spaceId: String, task: TaskDto): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(task)
            post(ip, port, "/spaces/$spaceId/tasks", body)
            true
        } catch (e: Exception) {
            Log.w(TAG, "postTask failed: ${e.message}")
            false
        }
    }

    /** PUT /spaces/{id}/tasks/{taskId}  – update a task on the owner device */
    suspend fun putTask(ip: String, port: Int, spaceId: String, task: TaskDto): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(task)
            put(ip, port, "/spaces/$spaceId/tasks/${task.id}", body)
            true
        } catch (e: Exception) {
            Log.w(TAG, "putTask failed: ${e.message}")
            false
        }
    }

    /** POST /spaces/{id}/join  – request to join a space on a remote device */
    suspend fun joinSpace(ip: String, port: Int, spaceId: String, joinReq: JoinRequest): SpaceDto? = withContext(Dispatchers.IO) {
        try {
            val body = gson.toJson(joinReq)
            val response = post(ip, port, "/spaces/$spaceId/join", body)
            val type = object : com.google.gson.reflect.TypeToken<ApiResponse<SpaceDto>>() {}.type
            val parsed: ApiResponse<SpaceDto> = gson.fromJson(response, type)
            parsed.data
        } catch (e: Exception) {
            Log.w(TAG, "joinSpace failed: ${e.message}")
            null
        }
    }

    /** GET /sync/{spaceId}?since={timestamp} */
    suspend fun sync(ip: String, port: Int, spaceId: String, since: Long): SyncData? = withContext(Dispatchers.IO) {
        try {
            val response = get(ip, port, "/sync/$spaceId?since=$since")
            val type = object : com.google.gson.reflect.TypeToken<ApiResponse<SyncData>>() {}.type
            val parsed: ApiResponse<SyncData> = gson.fromJson(response, type)
            parsed.data
        } catch (e: Exception) {
            Log.w(TAG, "sync failed: ${e.message}")
            null
        }
    }

    // ─── Low-level HTTP helpers ──────────────────────────────────────────────

    private fun get(ip: String, port: Int, path: String): String {
        return request(ip, port, "GET", path, null)
    }

    private fun post(ip: String, port: Int, path: String, body: String): String {
        return request(ip, port, "POST", path, body)
    }

    private fun put(ip: String, port: Int, path: String, body: String): String {
        return request(ip, port, "PUT", path, body)
    }

    private fun request(ip: String, port: Int, method: String, path: String, body: String?): String {
        Socket(ip, port).use { socket ->
            socket.soTimeout = TIMEOUT_MS
            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            val bodyBytes = body?.toByteArray() ?: ByteArray(0)
            writer.print("$method $path HTTP/1.1\r\n")
            writer.print("Host: $ip:$port\r\n")
            writer.print("Content-Type: application/json\r\n")
            writer.print("Content-Length: ${bodyBytes.size}\r\n")
            writer.print("Connection: close\r\n")
            writer.print("\r\n")
            if (body != null) writer.print(body)
            writer.flush()

            // Skip status line and headers
            var line = reader.readLine()
            while (!line.isNullOrEmpty()) {
                line = reader.readLine()
            }
            return reader.readText()
        }
    }
}
