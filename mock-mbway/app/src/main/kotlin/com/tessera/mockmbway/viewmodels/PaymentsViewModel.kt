package com.tessera.mockmbway.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.mockmbway.data.WebhookSender
import com.tessera.mockmbway.shared.PendingPayment
import com.tessera.mockmbway.shared.PendingPaymentsState
import com.tessera.mockmbway.shared.Resolution
import kotlinx.coroutines.launch

class PaymentsViewModel : ViewModel() {

    val pending = PendingPaymentsState.pending
    val history = PendingPaymentsState.history

    fun accept(payment: PendingPayment) = resolve(payment, Resolution.ACCEPTED)
    fun decline(payment: PendingPayment) = resolve(payment, Resolution.DECLINED)

    private fun resolve(payment: PendingPayment, resolution: Resolution) {
        val resolved = PendingPaymentsState.resolve(payment.transactionId, resolution)
        if (resolved == null) {
            Log.w(TAG, "resolve($resolution) — payment ${payment.transactionId} not found")
            return
        }
        viewModelScope.launch {
            WebhookSender.notify(resolved, resolution)
        }
    }

    private companion object {
        const val TAG = "PaymentsViewModel"
    }
}
