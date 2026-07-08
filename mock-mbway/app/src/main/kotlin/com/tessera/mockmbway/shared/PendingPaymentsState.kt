package com.tessera.mockmbway.shared

import androidx.compose.runtime.mutableStateListOf

data class PendingPayment(
    val transactionId: String,
    val transactionSignature: String,
    val merchantTransactionId: String,
    val terminalId: Int?,
    val description: String?,
    val amount: Double,
    val currency: String,
    val callbackUrl: String?,
    val customerPhone: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + DEFAULT_EXPIRY_MS,
) {
    companion object {
        const val DEFAULT_EXPIRY_MS = 120_000L
    }
}

enum class Resolution { ACCEPTED, DECLINED, EXPIRED }

data class ResolvedPayment(
    val transactionId: String,
    val merchantTransactionId: String,
    val description: String?,
    val amount: Double,
    val currency: String,
    val resolution: Resolution,
    val resolvedAt: Long = System.currentTimeMillis(),
)

object PendingPaymentsState {

    private const val HISTORY_LIMIT = 10

    val pending = mutableStateListOf<PendingPayment>()
    val history = mutableStateListOf<ResolvedPayment>()

    fun find(transactionId: String): PendingPayment? =
        pending.firstOrNull { it.transactionId == transactionId }

    fun resolve(transactionId: String, resolution: Resolution): PendingPayment? {
        val payment = find(transactionId) ?: return null
        pending.removeAll { it.transactionId == transactionId }
        history.add(0, ResolvedPayment(
            transactionId = payment.transactionId,
            merchantTransactionId = payment.merchantTransactionId,
            description = payment.description,
            amount = payment.amount,
            currency = payment.currency,
            resolution = resolution,
        ))
        while (history.size > HISTORY_LIMIT) {
            history.removeAt(history.size - 1)
        }
        return payment
    }

    fun expiredNow(): List<PendingPayment> {
        val now = System.currentTimeMillis()
        return pending.filter { it.expiresAt < now }.toList()
    }
}
