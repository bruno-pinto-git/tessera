package com.tessera.mockmbway.data

import android.util.Log
import com.tessera.mockmbway.shared.PendingPaymentsState
import com.tessera.mockmbway.shared.Resolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object PaymentExpiryScheduler {

    private const val TAG = "PaymentExpiry"
    private const val POLL_INTERVAL_MS = 5_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                val expired = PendingPaymentsState.expiredNow()
                if (expired.isNotEmpty()) {
                    Log.i(TAG, "Expiring ${expired.size} payment(s)")
                    expired.forEach { payment ->
                        PendingPaymentsState.resolve(payment.transactionId, Resolution.EXPIRED)
                        WebhookSender.notify(payment, Resolution.EXPIRED)
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
