package com.taskcenter.app.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.taskcenter.app.data.database.entity.Task
import com.taskcenter.app.data.database.entity.TaskStatus

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE spaceId = :spaceId ORDER BY createdAt DESC")
    fun getTasksForSpace(spaceId: String): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE spaceId = :spaceId ORDER BY createdAt DESC")
    suspend fun getTasksForSpaceList(spaceId: String): List<Task>

    @Query("SELECT * FROM tasks WHERE spaceId = :spaceId AND createdAt > :since ORDER BY createdAt ASC")
    suspend fun getTasksForSpaceSince(spaceId: String, since: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): Task?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskByIdLive(id: String): LiveData<Task?>

    @Query("SELECT * FROM tasks WHERE assignedToId = :userId AND status = 'IN_PROGRESS'")
    fun getTasksAssignedToUser(userId: String): LiveData<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks WHERE spaceId = :spaceId")
    suspend fun deleteBySpaceId(spaceId: String)
}
