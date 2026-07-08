package com.tessera.mockmbway.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.mockmbway.data.RelayPoller
import com.tessera.mockmbway.shared.RelayConfig
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    var host by mutableStateOf(RelayConfig.host)
        private set

    var secret by mutableStateOf(RelayConfig.secret)
        private set

    var saved by mutableStateOf(false)
        private set

    var resolving by mutableStateOf(false)
        private set

    var resolvedMode by mutableStateOf(RelayConfig.mode)
        private set

    fun onHostChange(value: String) {
        host = value
        saved = false
    }

    fun onSecretChange(value: String) {
        secret = value
        saved = false
    }

    fun save() {
        RelayConfig.update(getApplication(), host, secret)
        host = RelayConfig.host
        secret = RelayConfig.secret
        viewModelScope.launch {
            resolving = true
            RelayPoller.pollNow(getApplication())
            resolvedMode = RelayConfig.mode
            resolving = false
            saved = true
        }
    }
}
