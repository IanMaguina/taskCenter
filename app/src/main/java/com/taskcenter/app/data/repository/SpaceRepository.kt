package com.taskcenter.app.data.repository

import android.content.Context
import com.taskcenter.app.data.database.AppDatabase
import com.taskcenter.app.data.database.entity.Space
import com.taskcenter.app.data.database.entity.SpaceMember
import com.taskcenter.app.network.JoinRequest
import com.taskcenter.app.network.MemberDto
import com.taskcenter.app.network.SpaceDto
import com.taskcenter.app.network.TaskCenterClient
import java.util.UUID

class SpaceRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val spaceDao = db.spaceDao()
    private val memberDao = db.spaceMemberDao()

    val allSpaces get() = spaceDao.getAllSpaces()

    fun getSpaceLive(id: String) = spaceDao.getSpaceByIdLive(id)

    fun getMembersLive(spaceId: String) = memberDao.getMembersForSpace(spaceId)

    suspend fun getSpaceById(id: String) = spaceDao.getSpaceById(id)

    suspend fun createSpace(
        name: String,
        description: String,
        hasRewards: Boolean,
        ownerId: String,
        ownerName: String,
        ownerIp: String,
        ownerPort: Int
    ): Space {
        val space = Space(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            ownerId = ownerId,
            ownerName = ownerName,
            hasRewards = hasRewards,
            ownerIp = ownerIp,
            ownerPort = ownerPort
        )
        spaceDao.insert(space)
        // Auto-add owner as a member
        memberDao.insert(
            SpaceMember(
                spaceId = space.id,
                userId = ownerId,
                userName = ownerName,
                deviceIp = ownerIp,
                devicePort = ownerPort
            )
        )
        return space
    }

    suspend fun saveSpace(space: Space) = spaceDao.insert(space)

    suspend fun deleteSpace(spaceId: String) {
        spaceDao.deleteById(spaceId)
        memberDao.deleteBySpaceId(spaceId)
    }

    /** Join a remote space by connecting to its owner's device. */
    suspend fun joinRemoteSpace(
        ownerIp: String,
        ownerPort: Int,
        spaceId: String,
        userId: String,
        userName: String,
        myIp: String,
        myPort: Int
    ): Space? {
        val dto = TaskCenterClient.joinSpace(
            ownerIp, ownerPort, spaceId,
            JoinRequest(userId, userName, myIp, myPort)
        ) ?: return null
        val space = dto.toEntity()
        spaceDao.insert(space)
        memberDao.insert(SpaceMember(spaceId = spaceId, userId = userId, userName = userName,
            deviceIp = myIp, devicePort = myPort))
        return space
    }

    suspend fun saveMember(member: SpaceMember) = memberDao.insert(member)

    suspend fun saveMembersFromDto(members: List<MemberDto>) =
        memberDao.insertAll(members.map { it.toEntity() })

    suspend fun getAllSpacesList() = spaceDao.getAllSpacesList()
}
