package com.tessera.mockmbway.shared

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tessera.mockmbway.data.RelayConfigStore

/**
 * Where the mock polls for pending payments and posts webhook callbacks —
 * a single host, since both the poll and the webhook always live on the
 * same ticket-service. [RelayPoller] resolves [mode] by trying the LAN-dev
 * shape (http, port 8081) then the deployed shape (https, no port); once
 * resolved it's remembered until the next [update].
 */
object RelayConfig {

    const val DEFAULT_HOST = "192.168.1.61"
    const val DEFAULT_SECRET = "dev-secret"

    enum class Mode { LOCAL, REMOTE, UNKNOWN }

    var host by mutableStateOf(DEFAULT_HOST)
        private set

    var secret by mutableStateOf(DEFAULT_SECRET)
        private set

    var mode by mutableStateOf(Mode.UNKNOWN)

    var lastPollOk by mutableStateOf<Boolean?>(null)
    var lastPollAt by mutableStateOf<Long?>(null)

    val baseUrl: String
        get() = if (mode == Mode.REMOTE) "https://$host" else "http://$host:8081"

    fun load(context: Context) {
        val store = RelayConfigStore(context)
        host = store.loadHost()?.takeIf { it.isNotBlank() } ?: DEFAULT_HOST
        secret = store.loadSecret()?.takeIf { it.isNotBlank() } ?: DEFAULT_SECRET
        mode = Mode.UNKNOWN
    }

    fun update(context: Context, rawHost: String, rawSecret: String) {
        host = sanitize(rawHost)
        secret = rawSecret.trim().ifBlank { DEFAULT_SECRET }
        mode = Mode.UNKNOWN
        lastPollOk = null
        lastPollAt = null
        RelayConfigStore(context).save(host, secret)
    }

    fun sanitize(rawHost: String): String =
        rawHost.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore(':')
}
