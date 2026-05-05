package com.taskcenter.app.data.repository

import android.content.Context
import com.taskcenter.app.data.database.AppDatabase
import com.taskcenter.app.data.database.entity.Task
import com.taskcenter.app.data.database.entity.TaskStatus
import com.taskcenter.app.network.TaskCenterClient
import com.taskcenter.app.network.TaskDto
import java.util.UUID

class TaskRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val taskDao = db.taskDao()
    private val spaceDao = db.spaceDao()

    fun getTasksForSpace(spaceId: String) = taskDao.getTasksForSpace(spaceId)

    fun getTaskLive(id: String) = taskDao.getTaskByIdLive(id)

    fun getTasksAssignedToUser(userId: String) = taskDao.getTasksAssignedToUser(userId)

    suspend fun getTaskById(id: String) = taskDao.getTaskById(id)

    /**
     * Create a new task. If this device is not the space owner, also push the task
     * to the owner's server so it's persisted centrally.
     */
    suspend fun createTask(
        spaceId: String,
        name: String,
        description: String,
        estimatedTime: Int,
        reward: Int,
        currentUserId: String
    ): Task {
        val task = Task(
            id = UUID.randomUUID().toString(),
            spaceId = spaceId,
            name = name,
            description = description,
            estimatedTime = estimatedTime,
            reward = reward,
            status = TaskStatus.PENDING
        )
        taskDao.insert(task)
        // Push to space owner if remote
        pushTaskToOwner(spaceId, TaskDto.from(task))
        return task
    }

    /** Assign this task to the given user (take the task). */
    suspend fun takeTask(taskId: String, userId: String, userName: String): Task? {
        val task = taskDao.getTaskById(taskId) ?: return null
        if (task.status != TaskStatus.PENDING) return task
        val updated = task.copy(
            status = TaskStatus.IN_PROGRESS,
            assignedToId = userId,
            assignedToName = userName,
            startedAt = System.currentTimeMillis()
        )
        taskDao.update(updated)
        // Push update to owner
        pushTaskToOwner(updated.spaceId, TaskDto.from(updated), isUpdate = true)
        return updated
    }

    /** Mark a task as completed, computing real time. */
    suspend fun completeTask(taskId: String): Task? {
        val task = taskDao.getTaskById(taskId) ?: return null
        val completedAt = System.currentTimeMillis()
        val realTimeMinutes = if (task.startedAt != null) {
            (completedAt - task.startedAt) / 60000
        } else 0L
        val updated = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = completedAt,
            realTime = realTimeMinutes
        )
        taskDao.update(updated)
        pushTaskToOwner(updated.spaceId, TaskDto.from(updated), isUpdate = true)
        return updated
    }

    suspend fun saveTask(task: Task) = taskDao.insert(task)

    suspend fun saveTasksFromDto(tasks: List<TaskDto>) =
        taskDao.insertAll(tasks.map { it.toEntity() })

    private suspend fun pushTaskToOwner(spaceId: String, dto: TaskDto, isUpdate: Boolean = false) {
        val space = spaceDao.getSpaceById(spaceId) ?: return
        if (space.ownerIp.isEmpty()) return
        if (isUpdate) {
            TaskCenterClient.putTask(space.ownerIp, space.ownerPort, spaceId, dto)
        } else {
            TaskCenterClient.postTask(space.ownerIp, space.ownerPort, spaceId, dto)
        }
    }
}
