package com.tessera.android.shared

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tessera.android.data.ServerConfigStore

object ServerConfig {

    const val DEFAULT_HOST = "192.168.1.61"
    const val PORT_GATEWAY = 8000
    const val PORT_KEYCLOAK = 8180

    var host by mutableStateOf(DEFAULT_HOST)
        private set

    val baseUrl: String
        get() = "http://$host:$PORT_GATEWAY"

    val issuer: String
        get() = "http://$host:$PORT_KEYCLOAK/realms/tessera"

    fun load(context: Context) {
        host = ServerConfigStore(context).load()?.takeIf { it.isNotBlank() } ?: DEFAULT_HOST
    }

    fun update(context: Context, rawHost: String) {
        val clean = sanitize(rawHost)
        host = clean
        ServerConfigStore(context).save(clean)
    }

    fun sanitize(rawHost: String): String =
        rawHost.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore(':')
}
