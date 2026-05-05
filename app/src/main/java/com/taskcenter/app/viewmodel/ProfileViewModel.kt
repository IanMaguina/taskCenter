package com.taskcenter.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.taskcenter.app.data.database.entity.User
import com.taskcenter.app.data.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepo = UserRepository(application)

    val currentUser = userRepo.currentUserLive

    private val _saved = MutableLiveData(false)
    val saved get() = _saved

    private val _error = MutableLiveData<String?>()
    val error get() = _error

    fun saveProfile(name: String) {
        if (name.isBlank()) { _error.value = "El nombre no puede estar vacío"; return }
        viewModelScope.launch {
            try {
                val existing = userRepo.getCurrentUser()
                if (existing == null) userRepo.createUser(name.trim())
                else userRepo.updateName(name.trim())
                _saved.value = true
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearSaved() = _saved.postValue(false)
    fun clearError() = _error.postValue(null)
}
