package com.tessera.android.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.tessera.android.shared.ServerConfig

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    var host by mutableStateOf(ServerConfig.host)
        private set

    var saved by mutableStateOf(false)
        private set

    val previewBaseUrl: String
        get() = "http://${ServerConfig.sanitize(host)}:${ServerConfig.PORT_GATEWAY}"

    val previewIssuer: String
        get() = "http://${ServerConfig.sanitize(host)}:${ServerConfig.PORT_KEYCLOAK}/realms/tessera"

    fun onHostChange(value: String) {
        host = value
        saved = false
    }

    fun save() {
        ServerConfig.update(getApplication(), host)
        host = ServerConfig.host
        saved = true
    }
}
