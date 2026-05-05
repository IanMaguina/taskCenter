package com.taskcenter.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.taskcenter.app.data.database.entity.Space
import com.taskcenter.app.data.repository.SpaceRepository
import com.taskcenter.app.data.repository.UserRepository
import com.taskcenter.app.network.TaskCenterClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val spaceRepo = SpaceRepository(application)
    private val userRepo = UserRepository(application)

    val allSpaces: LiveData<List<Space>> = spaceRepo.allSpaces
    val currentUser = userRepo.currentUserLive

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /** Manually trigger a refresh: try to fetch spaces from already-known remote peers */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            try {
                val spaces = spaceRepo.getAllSpacesList()
                for (space in spaces) {
                    if (space.ownerIp.isNotEmpty()) {
                        val remote = TaskCenterClient.getSpaces(space.ownerIp, space.ownerPort)
                        remote.forEach { dto ->
                            if (spaceRepo.getSpaceById(dto.id) == null) {
                                spaceRepo.saveSpace(dto.toEntity())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _error.postValue(e.message)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun clearError() = _error.postValue(null)
}
