package com.taskcenter.app.network

import com.google.gson.Gson
import com.taskcenter.app.data.database.entity.Space
import com.taskcenter.app.data.database.entity.SpaceMember
import com.taskcenter.app.data.database.entity.Task

// ─── Shared DTOs ────────────────────────────────────────────────────────────

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

data class SpaceDto(
    val id: String,
    val name: String,
    val description: String,
    val ownerId: String,
    val ownerName: String,
    val hasRewards: Boolean,
    val createdAt: Long,
    val ownerIp: String,
    val ownerPort: Int
) {
    fun toEntity() = Space(
        id = id, name = name, description = description,
        ownerId = ownerId, ownerName = ownerName, hasRewards = hasRewards,
        createdAt = createdAt, ownerIp = ownerIp, ownerPort = ownerPort
    )

    companion object {
        fun from(space: Space) = SpaceDto(
            id = space.id, name = space.name, description = space.description,
            ownerId = space.ownerId, ownerName = space.ownerName,
            hasRewards = space.hasRewards, createdAt = space.createdAt,
            ownerIp = space.ownerIp, ownerPort = space.ownerPort
        )
    }
}

data class TaskDto(
    val id: String,
    val spaceId: String,
    val name: String,
    val description: String,
    val estimatedTime: Int,
    val realTime: Long,
    val status: String,
    val assignedToId: String?,
    val assignedToName: String?,
    val reward: Int,
    val createdAt: Long,
    val startedAt: Long?,
    val completedAt: Long?
) {
    fun toEntity() = Task(
        id = id, spaceId = spaceId, name = name, description = description,
        estimatedTime = estimatedTime, realTime = realTime,
        status = com.taskcenter.app.data.database.entity.TaskStatus.valueOf(status),
        assignedToId = assignedToId, assignedToName = assignedToName,
        reward = reward, createdAt = createdAt, startedAt = startedAt,
        completedAt = completedAt
    )

    companion object {
        fun from(task: Task) = TaskDto(
            id = task.id, spaceId = task.spaceId, name = task.name,
            description = task.description, estimatedTime = task.estimatedTime,
            realTime = task.realTime, status = task.status.name,
            assignedToId = task.assignedToId, assignedToName = task.assignedToName,
            reward = task.reward, createdAt = task.createdAt,
            startedAt = task.startedAt, completedAt = task.completedAt
        )
    }
}

data class MemberDto(
    val spaceId: String,
    val userId: String,
    val userName: String,
    val deviceIp: String,
    val devicePort: Int,
    val joinedAt: Long
) {
    fun toEntity() = SpaceMember(
        spaceId = spaceId, userId = userId, userName = userName,
        deviceIp = deviceIp, devicePort = devicePort, joinedAt = joinedAt
    )

    companion object {
        fun from(m: SpaceMember) = MemberDto(
            spaceId = m.spaceId, userId = m.userId, userName = m.userName,
            deviceIp = m.deviceIp, devicePort = m.devicePort, joinedAt = m.joinedAt
        )
    }
}

data class JoinRequest(
    val userId: String,
    val userName: String,
    val deviceIp: String,
    val devicePort: Int
)

data class SyncData(
    val tasks: List<TaskDto>,
    val members: List<MemberDto>,
    val timestamp: Long
)

val gson = Gson()
