package com.taskcenter.app.data.database.entity

import androidx.room.Entity

@Entity(tableName = "space_members", primaryKeys = ["spaceId", "userId"])
data class SpaceMember(
    val spaceId: String,
    val userId: String,
    val userName: String,
    val deviceIp: String = "",
    val devicePort: Int = 8765,
    val joinedAt: Long = System.currentTimeMillis()
)
