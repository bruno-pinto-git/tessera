package com.tessera.android.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.data.UserRepository
import com.tessera.android.data.dto.UserDto
import kotlinx.coroutines.launch

sealed interface AdminUsersState {
    data object Loading : AdminUsersState
    data class Success(val users: List<UserDto>) : AdminUsersState
    data object Error : AdminUsersState
}

class UsersAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UserRepository(application)

    var state by mutableStateOf<AdminUsersState>(AdminUsersState.Loading)
        private set
    var busy by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        state = AdminUsersState.Loading
        viewModelScope.launch {
            state = try {
                AdminUsersState.Success(repo.list())
            } catch (e: Exception) {
                AdminUsersState.Error
            }
        }
    }

    fun createUser(username: String, email: String, firstName: String, lastName: String, password: String, role: String) {
        busy = true
        viewModelScope.launch {
            try {
                repo.create(username, email, firstName, lastName, password, role)
                state = AdminUsersState.Success(repo.list())
            } catch (e: Exception) {
                state = AdminUsersState.Error
            }
            busy = false
        }
    }

    fun updateUser(id: String, email: String, firstName: String, lastName: String, role: String?) {
        busy = true
        viewModelScope.launch {
            try {
                repo.updateUser(id, email, firstName, lastName, role)
                state = AdminUsersState.Success(repo.list())
            } catch (e: Exception) {
                state = AdminUsersState.Error
            }
            busy = false
        }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        busy = true
        viewModelScope.launch {
            try {
                repo.setEnabled(id, enabled)
                state = AdminUsersState.Success(repo.list())
            } catch (e: Exception) {
                state = AdminUsersState.Error
            }
            busy = false
        }
    }

    fun forcePasswordReset(id: String) {
        busy = true
        viewModelScope.launch {
            try {
                repo.forcePasswordReset(id)
            } catch (e: Exception) {
                state = AdminUsersState.Error
            }
            busy = false
        }
    }

    fun deleteUser(id: String) {
        busy = true
        viewModelScope.launch {
            try {
                repo.delete(id)
                state = AdminUsersState.Success(repo.list())
            } catch (e: Exception) {
                state = AdminUsersState.Error
            }
            busy = false
        }
    }
}
