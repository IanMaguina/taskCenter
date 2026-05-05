package com.taskcenter.app.network

import android.util.Log
import com.taskcenter.app.data.database.dao.SpaceDao
import com.taskcenter.app.data.database.dao.SpaceMemberDao
import com.taskcenter.app.data.database.dao.TaskDao
import com.taskcenter.app.data.database.entity.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

/**
 * Lightweight HTTP-like server that listens on [port].
 * Handles REST requests from other TaskCenter devices on the LAN.
 */
class TaskCenterServer(
    private val port: Int,
    private val spaceDao: SpaceDao,
    private val taskDao: TaskDao,
    private val memberDao: SpaceMemberDao,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        running = true
        scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                Log.d(TAG, "Server started on port $port")
                while (running) {
                    val client = serverSocket?.accept() ?: break
                    scope.launch(Dispatchers.IO) { handleClient(client) }
                }
            } catch (e: Exception) {
                if (running) Log.e(TAG, "Server error: ${e.message}")
            }
        }
    }

    fun stop() {
        running = false
        serverSocket?.close()
        serverSocket = null
        Log.d(TAG, "Server stopped")
    }

    private suspend fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            // Read request line
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) { sendError(writer, 400, "Bad Request"); return }

            val method = parts[0]
            val path = parts[1]

            // Read headers
            val headers = mutableMapOf<String, String>()
            var line = reader.readLine()
            while (!line.isNullOrEmpty()) {
                val idx = line.indexOf(':')
                if (idx > 0) headers[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
                line = reader.readLine()
            }

            // Read body if present
            val contentLength = headers["Content-Length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                reader.read(chars, 0, contentLength)
                String(chars)
            } else ""

            Log.d(TAG, "Request: $method $path")
            route(method, path, body, writer)

        } catch (e: Exception) {
            Log.e(TAG, "Client handling error: ${e.message}")
        } finally {
            socket.close()
        }
    }

    private suspend fun route(method: String, path: String, body: String, writer: PrintWriter) {
        val segments = path.split("/").filter { it.isNotEmpty() }

        when {
            // GET /spaces  – list all spaces hosted on this device
            method == "GET" && segments == listOf("spaces") -> {
                val spaces = spaceDao.getAllSpacesList().map { SpaceDto.from(it) }
                sendJson(writer, ApiResponse(true, spaces))
            }

            // GET /spaces/{id}
            method == "GET" && segments.size == 2 && segments[0] == "spaces" -> {
                val space = spaceDao.getSpaceById(segments[1])
                if (space != null) sendJson(writer, ApiResponse(true, SpaceDto.from(space)))
                else sendError(writer, 404, "Space not found")
            }

            // GET /spaces/{id}/tasks
            method == "GET" && segments.size == 3 && segments[0] == "spaces" && segments[2] == "tasks" -> {
                val tasks = taskDao.getTasksForSpaceList(segments[1]).map { TaskDto.from(it) }
                sendJson(writer, ApiResponse(true, tasks))
            }

            // POST /spaces/{id}/tasks  – add a new task
            method == "POST" && segments.size == 3 && segments[0] == "spaces" && segments[2] == "tasks" -> {
                val dto = gson.fromJson(body, TaskDto::class.java)
                taskDao.insert(dto.toEntity())
                sendJson(writer, ApiResponse<Unit>(true))
            }

            // PUT /spaces/{id}/tasks/{taskId}  – update a task (take / complete)
            method == "PUT" && segments.size == 4 && segments[0] == "spaces" && segments[2] == "tasks" -> {
                val dto = gson.fromJson(body, TaskDto::class.java)
                taskDao.insert(dto.toEntity())   // REPLACE strategy
                sendJson(writer, ApiResponse<Unit>(true))
            }

            // GET /spaces/{id}/members
            method == "GET" && segments.size == 3 && segments[0] == "spaces" && segments[2] == "members" -> {
                val members = memberDao.getMembersForSpaceList(segments[1]).map { MemberDto.from(it) }
                sendJson(writer, ApiResponse(true, members))
            }

            // POST /spaces/{id}/join  – another device requests to join
            method == "POST" && segments.size == 3 && segments[0] == "spaces" && segments[2] == "join" -> {
                val req = gson.fromJson(body, JoinRequest::class.java)
                val spaceId = segments[1]
                val space = spaceDao.getSpaceById(spaceId)
                if (space == null) { sendError(writer, 404, "Space not found"); return }
                val member = com.taskcenter.app.data.database.entity.SpaceMember(
                    spaceId = spaceId,
                    userId = req.userId,
                    userName = req.userName,
                    deviceIp = req.deviceIp,
                    devicePort = req.devicePort
                )
                memberDao.insert(member)
                // Return space details so the joining device can save it
                sendJson(writer, ApiResponse(true, SpaceDto.from(space)))
            }

            // GET /sync/{spaceId}?since={timestamp}
            method == "GET" && segments.size == 2 && segments[0] == "sync" -> {
                val spaceId = segments[1]
                // Extract since from query string in path (path may have ?since=...)
                val since = path.substringAfter("since=", "0").toLongOrNull() ?: 0L
                val tasks = taskDao.getTasksForSpaceSince(spaceId, since).map { TaskDto.from(it) }
                val members = memberDao.getMembersForSpaceList(spaceId).map { MemberDto.from(it) }
                sendJson(writer, ApiResponse(true, SyncData(tasks, members, System.currentTimeMillis())))
            }

            else -> sendError(writer, 404, "Not Found")
        }
    }

    private fun sendJson(writer: PrintWriter, data: Any) {
        val json = gson.toJson(data)
        writer.print("HTTP/1.1 200 OK\r\n")
        writer.print("Content-Type: application/json\r\n")
        writer.print("Content-Length: ${json.toByteArray().size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(json)
        writer.flush()
    }

    private fun sendError(writer: PrintWriter, code: Int, message: String) {
        val json = gson.toJson(ApiResponse<Unit>(false, error = message))
        writer.print("HTTP/1.1 $code $message\r\n")
        writer.print("Content-Type: application/json\r\n")
        writer.print("Content-Length: ${json.toByteArray().size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.print(json)
        writer.flush()
    }

    companion object {
        private const val TAG = "TaskCenterServer"
    }
}
