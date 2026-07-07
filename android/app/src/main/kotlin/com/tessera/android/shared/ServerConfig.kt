package com.tessera.android.shared

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tessera.android.data.ServerConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import java.util.concurrent.TimeUnit

object ServerConfig {

    const val DEFAULT_HOST = "192.168.1.61"
    const val PORT_GATEWAY = 8000
    const val PORT_KEYCLOAK = 8180
    private const val PROBE_PATH = "/api/v1/events"
    private const val PROBE_TIMEOUT_MS = 2500L

    enum class Mode { LOCAL, REMOTE, UNKNOWN }

    private val probeClient = OkHttp(
        OkHttpClient.Builder()
            .connectTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build(),
    )

    var host by mutableStateOf(DEFAULT_HOST)
        private set

    var mode by mutableStateOf(Mode.UNKNOWN)
        private set

    val baseUrl: String
        get() = if (mode == Mode.REMOTE) "https://$host" else "http://$host:$PORT_GATEWAY"

    val issuer: String
        get() = if (mode == Mode.REMOTE) {
            "https://$host/auth/realms/tessera"
        } else {
            "http://$host:$PORT_KEYCLOAK/realms/tessera"
        }

    fun load(context: Context) {
        host = ServerConfigStore(context).load()?.takeIf { it.isNotBlank() } ?: DEFAULT_HOST
        mode = Mode.UNKNOWN
    }

    fun update(context: Context, rawHost: String) {
        val clean = sanitize(rawHost)
        host = clean
        mode = Mode.UNKNOWN
        ServerConfigStore(context).save(clean)
    }

    fun sanitize(rawHost: String): String =
        rawHost.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .substringBefore(':')

    /**
     * Probes [host] with both known shapes — local dev (http, fixed ports) and
     * the public Azure deployment (https, no port, /auth prefix on Keycloak) —
     * and remembers whichever one actually answers, so baseUrl/issuer resolve
     * correctly either way without the user picking a scheme by hand.
     */
    suspend fun resolveMode(): Mode {
        mode = when {
            probe("http://$host:$PORT_GATEWAY$PROBE_PATH") -> Mode.LOCAL
            probe("https://$host$PROBE_PATH") -> Mode.REMOTE
            else -> Mode.UNKNOWN
        }
        return mode
    }

    private suspend fun probe(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { probeClient(Request(Method.GET, url)) }.isSuccess
    }
}
