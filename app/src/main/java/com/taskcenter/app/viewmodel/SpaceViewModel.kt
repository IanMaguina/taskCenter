package com.taskcenter.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.taskcenter.app.data.database.entity.Space
import com.taskcenter.app.data.database.entity.SpaceMember
import com.taskcenter.app.data.repository.SpaceRepository
import com.taskcenter.app.data.repository.UserRepository
import kotlinx.coroutines.launch

class SpaceViewModel(application: Application) : AndroidViewModel(application) {
    private val spaceRepo = SpaceRepository(application)
    private val userRepo = UserRepository(application)

    private val spaceId = MutableLiveData<String>()
    val space: LiveData<Space?> = spaceId.switchMap { id -> spaceRepo.getSpaceLive(id) }

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _createSuccess = MutableLiveData<Space?>()
    val createSuccess: LiveData<Space?> = _createSuccess

    private val _joinSuccess = MutableLiveData<Space?>()
    val joinSuccess: LiveData<Space?> = _joinSuccess

    fun loadSpace(id: String) {
        spaceId.value = id
    }

    fun getMembersLive(spaceId: String): LiveData<List<SpaceMember>> =
        spaceRepo.getMembersLive(spaceId)

    fun createSpace(
        name: String,
        description: String,
        hasRewards: Boolean,
        ownerIp: String,
        ownerPort: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = userRepo.getCurrentUser()
                    ?: run { _error.value = "Configura tu perfil primero"; return@launch }
                val space = spaceRepo.createSpace(
                    name = name,
                    description = description,
                    hasRewards = hasRewards,
                    ownerId = user.id,
                    ownerName = user.name,
                    ownerIp = ownerIp,
                    ownerPort = ownerPort
                )
                _createSuccess.value = space
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun joinSpace(ownerIp: String, ownerPort: Int, spaceId: String, myIp: String, myPort: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = userRepo.getCurrentUser()
                    ?: run { _error.value = "Configura tu perfil primero"; return@launch }
                val space = spaceRepo.joinRemoteSpace(
                    ownerIp = ownerIp,
                    ownerPort = ownerPort,
                    spaceId = spaceId,
                    userId = user.id,
                    userName = user.name,
                    myIp = myIp,
                    myPort = myPort
                )
                if (space != null) _joinSuccess.value = space
                else _error.value = "No se pudo unir al espacio"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteSpace(spaceId: String) {
        viewModelScope.launch {
            try { spaceRepo.deleteSpace(spaceId) }
            catch (e: Exception) { _error.value = e.message }
        }
    }

    fun clearError() = _error.postValue(null)
    fun clearCreateSuccess() = _createSuccess.postValue(null)
    fun clearJoinSuccess() = _joinSuccess.postValue(null)
}
