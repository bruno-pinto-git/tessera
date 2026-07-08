package com.tessera.mockmbway.shared

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tessera.mockmbway.data.RelayConfigStore

/**
 * Where the mock polls for pending payments and posts webhook callbacks.
 * [RelayPoller] resolves [mode] by trying [localHost] (LAN-dev shape: http,
 * port 8081) then the fixed [REMOTE_HOST] (deployed shape: https, no port);
 * once resolved it's remembered until the next [update]. The remote FQDN
 * never changes (it's the fixed Azure DNS label), so — unlike the local
 * IP, which does change across networks/demos — it's never something the
 * operator should need to type in.
 */
object RelayConfig {

    const val DEFAULT_LOCAL_HOST = "192.168.1.61"
    const val DEFAULT_SECRET = "dev-secret"

    /** Fixed Azure DNS label — [RelayPoller] also reads this directly to try the remote shape. */
    const val REMOTE_HOST = "tessera.swedencentral.cloudapp.azure.com"

    enum class Mode { LOCAL, REMOTE, UNKNOWN }

    /** Only ever the LOCAL dev host — Settings never touches the remote one. */
    var localHost by mutableStateOf(DEFAULT_LOCAL_HOST)
        private set

    var secret by mutableStateOf(DEFAULT_SECRET)
        private set

    var mode by mutableStateOf(Mode.UNKNOWN)

    var lastPollOk by mutableStateOf<Boolean?>(null)
    var lastPollAt by mutableStateOf<Long?>(null)

    val baseUrl: String
        get() = if (mode == Mode.REMOTE) "https://$REMOTE_HOST" else "http://$localHost:8081"

    /** Whichever host is actually active right now, for status display. */
    val activeHost: String
        get() = if (mode == Mode.REMOTE) REMOTE_HOST else localHost

    fun load(context: Context) {
        val store = RelayConfigStore(context)
        localHost = store.loadHost()?.takeIf { it.isNotBlank() } ?: DEFAULT_LOCAL_HOST
        secret = store.loadSecret()?.takeIf { it.isNotBlank() } ?: DEFAULT_SECRET
        mode = Mode.UNKNOWN
    }

    fun update(context: Context, rawHost: String, rawSecret: String) {
        localHost = sanitize(rawHost)
        secret = rawSecret.trim().ifBlank { DEFAULT_SECRET }
        mode = Mode.UNKNOWN
        lastPollOk = null
        lastPollAt = null
        RelayConfigStore(context).save(localHost, secret)
    }

    fun sanitize(rawHost: String): String =
        rawHost.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore(':')
}
