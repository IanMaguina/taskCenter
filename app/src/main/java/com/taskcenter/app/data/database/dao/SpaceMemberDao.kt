package com.taskcenter.app.data.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.taskcenter.app.data.database.entity.SpaceMember

@Dao
interface SpaceMemberDao {
    @Query("SELECT * FROM space_members WHERE spaceId = :spaceId")
    fun getMembersForSpace(spaceId: String): LiveData<List<SpaceMember>>

    @Query("SELECT * FROM space_members WHERE spaceId = :spaceId")
    suspend fun getMembersForSpaceList(spaceId: String): List<SpaceMember>

    @Query("SELECT * FROM space_members WHERE spaceId = :spaceId AND userId = :userId")
    suspend fun getMember(spaceId: String, userId: String): SpaceMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: SpaceMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<SpaceMember>)

    @Delete
    suspend fun delete(member: SpaceMember)

    @Query("DELETE FROM space_members WHERE spaceId = :spaceId AND userId = :userId")
    suspend fun deleteBySpaceAndUser(spaceId: String, userId: String)

    @Query("DELETE FROM space_members WHERE spaceId = :spaceId")
    suspend fun deleteBySpaceId(spaceId: String)
}
