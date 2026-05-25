package com.tessera.android.viewmodels

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.R
import com.tessera.android.data.KeycloakClient
import kotlinx.coroutines.launch

sealed interface LoginState {
    data object Idle : LoginState
    data object Submitting : LoginState
    data object Success : LoginState
    data class Error(val messageRes: Int) : LoginState
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "LoginViewModel"
    private val client = KeycloakClient(application)

    var state by mutableStateOf<LoginState>(LoginState.Idle)
        private set

    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    fun onUsernameChange(value: String) {
        username = value
        if (state is LoginState.Error) state = LoginState.Idle
    }

    fun onPasswordChange(value: String) {
        password = value
        if (state is LoginState.Error) state = LoginState.Idle
    }

    fun submit() {
        val user = username.trim()
        val pwd = password
        if (user.isEmpty() || pwd.isEmpty()) {
            state = LoginState.Error(R.string.login_error_empty)
            return
        }
        state = LoginState.Submitting
        viewModelScope.launch {
            val result = client.login(user, pwd)
            state = if (result.isSuccess) {
                password = ""
                LoginState.Success
            } else {
                Log.w(tag, "Login failed", result.exceptionOrNull())
                LoginState.Error(R.string.login_error_credentials)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.dispose()
    }
}
