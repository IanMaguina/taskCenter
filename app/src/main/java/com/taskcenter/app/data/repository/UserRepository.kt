package com.taskcenter.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.taskcenter.app.data.database.AppDatabase
import com.taskcenter.app.data.database.entity.User
import java.util.UUID

class UserRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("taskcenter_prefs", Context.MODE_PRIVATE)

    val currentUserLive get() = userDao.getCurrentUserLive()

    suspend fun getCurrentUser(): User? = userDao.getCurrentUser()

    suspend fun createUser(name: String): User {
        val user = User(
            id = prefs.getString("user_id", null) ?: UUID.randomUUID().toString().also {
                prefs.edit().putString("user_id", it).apply()
            },
            name = name,
            deviceId = getOrCreateDeviceId()
        )
        userDao.insert(user)
        return user
    }

    suspend fun updateName(name: String) {
        val current = userDao.getCurrentUser() ?: return
        userDao.update(current.copy(name = name))
    }

    private fun getOrCreateDeviceId(): String {
        return prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
    }
}
