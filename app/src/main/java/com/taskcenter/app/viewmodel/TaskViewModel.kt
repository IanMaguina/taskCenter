package com.taskcenter.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.taskcenter.app.data.database.entity.Task
import com.taskcenter.app.data.repository.TaskRepository
import com.taskcenter.app.data.repository.UserRepository
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val taskRepo = TaskRepository(application)
    private val userRepo = UserRepository(application)

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _taskSaved = MutableLiveData<Boolean>(false)
    val taskSaved: LiveData<Boolean> = _taskSaved

    fun getTasksForSpace(spaceId: String): LiveData<List<Task>> =
        taskRepo.getTasksForSpace(spaceId)

    fun getTaskLive(taskId: String): LiveData<Task?> =
        taskRepo.getTaskLive(taskId)

    fun getTasksAssignedToUser(userId: String) = taskRepo.getTasksAssignedToUser(userId)

    fun createTask(
        spaceId: String,
        name: String,
        description: String,
        estimatedTime: Int,
        reward: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = userRepo.getCurrentUser()
                    ?: run { _error.value = "Configura tu perfil primero"; return@launch }
                taskRepo.createTask(
                    spaceId = spaceId,
                    name = name,
                    description = description,
                    estimatedTime = estimatedTime,
                    reward = reward,
                    currentUserId = user.id
                )
                _taskSaved.value = true
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun takeTask(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = userRepo.getCurrentUser()
                    ?: run { _error.value = "Configura tu perfil primero"; return@launch }
                taskRepo.takeTask(taskId, user.id, user.name)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                taskRepo.completeTask(taskId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() = _error.postValue(null)
    fun clearTaskSaved() = _taskSaved.postValue(false)
}
