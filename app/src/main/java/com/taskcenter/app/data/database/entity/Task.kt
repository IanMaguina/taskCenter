package com.taskcenter.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String,
    val spaceId: String,
    val name: String,
    val description: String = "",
    /** Estimated time in minutes */
    val estimatedTime: Int = 0,
    /** Real time in minutes, calculated from startedAt to completedAt */
    val realTime: Long = 0,
    val status: TaskStatus = TaskStatus.PENDING,
    val assignedToId: String? = null,
    val assignedToName: String? = null,
    /** Optional reward points/coins if space has rewards enabled */
    val reward: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null
)
