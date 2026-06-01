package com.tessera.android.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.R
import com.tessera.android.data.AuthRequest
import com.tessera.android.data.KeycloakClient
import kotlinx.coroutines.launch

sealed interface LoginState {
    data object Loading : LoginState
    data class Authorizing(val url: String) : LoginState
    data object Exchanging : LoginState
    data object Success : LoginState
    data class Error(val messageRes: Int) : LoginState
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "LoginViewModel"
    private val client = KeycloakClient(application)
    private var pendingRequest: AuthRequest? = null

    var state by mutableStateOf<LoginState>(LoginState.Loading)
        private set

    init {
        start()
    }

    fun start() {
        state = LoginState.Loading
        viewModelScope.launch {
            try {
                val request = client.buildAuthRequest()
                pendingRequest = request
                state = LoginState.Authorizing(request.url)
            } catch (e: Throwable) {
                Log.e(tag, "Failed to build auth request", e)
                state = LoginState.Error(R.string.login_error_unknown)
            }
        }
    }

    fun onCallbackReceived(uri: Uri) {
        val request = pendingRequest
        if (request == null) {
            state = LoginState.Error(R.string.login_error_unknown)
            return
        }
        val error = uri.getQueryParameter("error")
        if (error != null) {
            Log.w(tag, "Auth callback error: $error")
            state = LoginState.Error(R.string.login_error_credentials)
            return
        }
        val code = uri.getQueryParameter("code")
        val receivedState = uri.getQueryParameter("state")
        if (code.isNullOrBlank() || receivedState != request.state) {
            Log.w(tag, "Invalid callback — code=$code, state matches=${receivedState == request.state}")
            state = LoginState.Error(R.string.login_error_unknown)
            return
        }
        state = LoginState.Exchanging
        viewModelScope.launch {
            val result = client.exchangeCode(code, request.verifier)
            state = if (result.isSuccess) {
                LoginState.Success
            } else {
                Log.w(tag, "Token exchange failed", result.exceptionOrNull())
                LoginState.Error(R.string.login_error_unknown)
            }
        }
    }

    fun retry() {
        start()
    }

    override fun onCleared() {
        super.onCleared()
        client.dispose()
    }
}
