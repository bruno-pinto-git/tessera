package com.tessera.mockmbway.data

import android.content.Context
import android.util.Log
import com.tessera.mockmbway.data.dto.RelayItem
import com.tessera.mockmbway.shared.PendingPayment
import com.tessera.mockmbway.shared.PendingPaymentsState
import com.tessera.mockmbway.shared.RelayConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Polls the backend for pending MB WAY payments instead of hosting a server
 * the backend calls into — the phone may be on a network the backend can't
 * reach, but the reverse (phone -> backend) always works. While
 * [RelayConfig.mode] is unresolved, each tick tries the LAN-dev shape then
 * the deployed shape and remembers whichever answers; a successful attempt
 * IS a real poll (any items it returns get delivered), so resolving the
 * mode and fetching pending payments are the same network call.
 */
object RelayPoller {

    private const val TAG = "RelayPoller"
    private const val POLL_INTERVAL_MS = 3_000L
    private const val REQUEST_TIMEOUT_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
    }

    fun start(context: Context) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                tick(context)
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    /** Forces an immediate tick (e.g. right after saving new settings) instead of waiting for the next scheduled one. */
    suspend fun pollNow(context: Context) = tick(context)

    private suspend fun tick(context: Context) {
        if (RelayConfig.mode == RelayConfig.Mode.UNKNOWN) {
            if (deliver(context, "http://${RelayConfig.localHost}:8081")) {
                RelayConfig.mode = RelayConfig.Mode.LOCAL
                return
            }
            if (deliver(context, "https://${RelayConfig.REMOTE_HOST}")) {
                RelayConfig.mode = RelayConfig.Mode.REMOTE
            }
            return
        }
        deliver(context, RelayConfig.baseUrl)
    }

    private suspend fun deliver(context: Context, base: String): Boolean =
        try {
            val items: List<RelayItem> = client.post("$base/api/v1/mbway/relay/poll") {
                header("X-Relay-Secret", RelayConfig.secret)
            }.body()
            RelayConfig.lastPollOk = true
            RelayConfig.lastPollAt = System.currentTimeMillis()
            items.forEach { deliverToUi(context, it) }
            true
        } catch (e: Exception) {
            RelayConfig.lastPollOk = false
            RelayConfig.lastPollAt = System.currentTimeMillis()
            Log.w(TAG, "poll to $base failed: ${e.message}")
            false
        }

    private fun deliverToUi(context: Context, item: RelayItem) {
        val payment = PendingPayment(
            transactionId = item.transactionID,
            transactionSignature = item.transactionSignature,
            merchantTransactionId = item.merchantTransactionId,
            terminalId = item.terminalId,
            description = item.description,
            amount = item.amount.value,
            currency = item.amount.currency,
            callbackUrl = item.callbackUrl,
            customerPhone = item.customerPhone,
        )
        PendingPaymentsState.pending.add(payment)
        Log.i(TAG, "Relay delivered ${payment.transactionId} (${payment.amount} ${payment.currency})")
        Sounder.incoming()
        PaymentNotifications.notify(context, payment)
    }
}
