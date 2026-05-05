package com.taskcenter.app.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.taskcenter.app.data.database.entity.Space

@Dao
interface SpaceDao {
    @Query("SELECT * FROM spaces ORDER BY createdAt DESC")
    fun getAllSpaces(): LiveData<List<Space>>

    @Query("SELECT * FROM spaces ORDER BY createdAt DESC")
    suspend fun getAllSpacesList(): List<Space>

    @Query("SELECT * FROM spaces WHERE id = :id")
    suspend fun getSpaceById(id: String): Space?

    @Query("SELECT * FROM spaces WHERE id = :id")
    fun getSpaceByIdLive(id: String): LiveData<Space?>

    @Query("SELECT * FROM spaces WHERE ownerId = :ownerId")
    fun getSpacesByOwner(ownerId: String): LiveData<List<Space>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(space: Space)

    @Update
    suspend fun update(space: Space)

    @Delete
    suspend fun delete(space: Space)

    @Query("DELETE FROM spaces WHERE id = :id")
    suspend fun deleteById(id: String)
}
