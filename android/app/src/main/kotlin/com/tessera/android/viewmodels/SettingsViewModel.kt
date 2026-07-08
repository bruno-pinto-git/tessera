package com.tessera.android.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.shared.ServerConfig
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    var host by mutableStateOf(ServerConfig.localHost)
        private set

    var saved by mutableStateOf(false)
        private set

    var resolving by mutableStateOf(false)
        private set

    var resolvedMode by mutableStateOf(ServerConfig.mode)
        private set

    fun onHostChange(value: String) {
        host = value
        saved = false
    }

    fun save() {
        ServerConfig.update(getApplication(), host)
        host = ServerConfig.localHost
        viewModelScope.launch {
            resolving = true
            resolvedMode = ServerConfig.resolveMode()
            resolving = false
            saved = true
        }
    }
}
