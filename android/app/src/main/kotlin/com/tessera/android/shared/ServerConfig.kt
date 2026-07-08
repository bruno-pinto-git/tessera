package com.tessera.android.shared

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tessera.android.data.ServerConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Request
import java.util.concurrent.TimeUnit

object ServerConfig {

    const val DEFAULT_LOCAL_HOST = "192.168.1.61"
    const val PORT_GATEWAY = 8000
    const val PORT_KEYCLOAK = 8180
    private const val PROBE_PATH = "/api/v1/events"
    private const val PROBE_TIMEOUT_MS = 2500L

    /**
     * The deployed Azure FQDN never changes (it's the free Azure DNS label,
     * fixed for the life of the VM) — unlike the local dev IP, which does
     * change across networks/demos, this isn't something the user should
     * ever need to type into Settings.
     */
    private const val REMOTE_HOST = "tessera.swedencentral.cloudapp.azure.com"

    enum class Mode { LOCAL, REMOTE, UNKNOWN }

    /**
     * Outlives any single screen — [resolveModeInBackground] is fired from
     * WelcomeScreen, which navigates itself away (and cancels its own
     * LaunchedEffects) after a fixed splash delay that can easily be shorter
     * than two sequential network probes. Running here instead means the
     * probe keeps going and still updates [mode] even if the splash is long gone.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val probeClient = OkHttp(
        OkHttpClient.Builder()
            .connectTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build(),
    )

    /** Only ever the LOCAL dev host — Settings never touches the remote one. */
    var localHost by mutableStateOf(DEFAULT_LOCAL_HOST)
        private set

    var mode by mutableStateOf(Mode.UNKNOWN)
        private set

    val baseUrl: String
        get() = if (mode == Mode.REMOTE) "https://$REMOTE_HOST" else "http://$localHost:$PORT_GATEWAY"

    val issuer: String
        get() = if (mode == Mode.REMOTE) {
            "https://$REMOTE_HOST/auth/realms/tessera"
        } else {
            "http://$localHost:$PORT_KEYCLOAK/realms/tessera"
        }

    fun load(context: Context) {
        localHost = ServerConfigStore(context).load()?.takeIf { it.isNotBlank() } ?: DEFAULT_LOCAL_HOST
        mode = Mode.UNKNOWN
    }

    fun update(context: Context, rawHost: String) {
        val clean = sanitize(rawHost)
        localHost = clean
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
     * Probes the configured local host, then the fixed remote FQDN, and
     * remembers whichever one actually answers, so baseUrl/issuer resolve
     * correctly either way without the user picking a scheme by hand — and
     * without ever needing to hand-enter the remote address, since it's
     * always the same one.
     */
    suspend fun resolveMode(): Mode {
        mode = when {
            probe("http://$localHost:$PORT_GATEWAY$PROBE_PATH") -> Mode.LOCAL
            probe("https://$REMOTE_HOST$PROBE_PATH") -> Mode.REMOTE
            else -> Mode.UNKNOWN
        }
        Log.i("ServerConfig", "resolveMode() localHost=$localHost -> $mode")
        return mode
    }

    /** Fire-and-forget entry point for call sites that can't await — see [scope]. */
    fun resolveModeInBackground() {
        scope.launch { resolveMode() }
    }

    private suspend fun probe(url: String): Boolean = withContext(Dispatchers.IO) {
        val result = runCatching { probeClient(Request(Method.GET, url)) }
        result.onSuccess { Log.i("ServerConfig", "probe($url) -> HTTP ${it.status.code}") }
        result.onFailure { Log.w("ServerConfig", "probe($url) failed: ${it.javaClass.simpleName}: ${it.message}") }
        // A 5xx means something answered but isn't actually working (e.g. a
        // reverse proxy timing out on a dead backend) - that shouldn't count
        // as "this is the right endpoint", only a non-5xx response should.
        result.getOrNull()?.let { it.status.code < 500 } ?: false
    }
}
