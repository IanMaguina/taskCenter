package com.taskcenter.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "spaces")
data class Space(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String = "",
    val ownerId: String,
    val ownerName: String,
    val hasRewards: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // Network info of the owner device
    val ownerIp: String = "",
    val ownerPort: Int = 8765
)
