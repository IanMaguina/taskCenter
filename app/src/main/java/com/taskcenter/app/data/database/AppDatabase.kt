package com.taskcenter.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.taskcenter.app.data.database.dao.SpaceDao
import com.taskcenter.app.data.database.dao.SpaceMemberDao
import com.taskcenter.app.data.database.dao.TaskDao
import com.taskcenter.app.data.database.dao.UserDao
import com.taskcenter.app.data.database.entity.Space
import com.taskcenter.app.data.database.entity.SpaceMember
import com.taskcenter.app.data.database.entity.Task
import com.taskcenter.app.data.database.entity.TaskStatus
import com.taskcenter.app.data.database.entity.User

class TaskStatusConverter {
    @TypeConverter
    fun fromStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TaskStatus = TaskStatus.valueOf(value)
}

@Database(
    entities = [User::class, Space::class, Task::class, SpaceMember::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(TaskStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun spaceDao(): SpaceDao
    abstract fun taskDao(): TaskDao
    abstract fun spaceMemberDao(): SpaceMemberDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskcenter.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
