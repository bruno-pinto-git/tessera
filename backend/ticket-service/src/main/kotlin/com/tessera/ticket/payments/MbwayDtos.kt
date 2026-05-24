package com.tessera.ticket.payments

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * DTOs that mirror the SIBS Payment Gateway protocol for MB WAY.
 *
 * These shapes are intentionally identical to what the production SIBS
 * gateway returns, so swapping the mock for the real gateway is a URL
 * change in config. See `mock-mbway/README.md` for the full contract.
 */

data class Amount(
    val value: Double,
    val currency: String,
)

data class ReturnStatus(
    val statusCode: String,
    val statusMsg: String,
    val statusDescription: String? = null,
)

// ---------- POST /api/v1/payments (create checkout) ----------

data class MerchantIn(
    val terminalId: Int? = null,
    val channel: String? = null,
    val merchantTransactionId: String,
    val transactionDescription: String? = null,
    val shopURL: String? = null,
    val callbackUrl: String? = null,
)

data class TransactionIn(
    val transactionTimestamp: String? = null,
    val description: String? = null,
    val moto: Boolean? = null,
    val paymentType: String,
    val paymentMethod: List<String>,
    val amount: Amount,
)

data class CreatePaymentRequest(
    val merchant: MerchantIn,
    val transaction: TransactionIn,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MerchantOut(
    val terminalId: Int? = null,
    val channel: String? = null,
    val merchantTransactionId: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreatePaymentResponse(
    val returnStatus: ReturnStatus,
    val transactionID: String,
    val transactionSignature: String,
    val amount: Amount? = null,
    val merchant: MerchantOut? = null,
    val paymentMethodList: List<String>? = null,
    val expiry: String? = null,
)

// ---------- POST /api/v1/payments/{id}/mbway/purchase ----------

data class MbwayPurchaseRequest(
    val customerPhone: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MbwayPurchaseResponse(
    val returnStatus: ReturnStatus,
    val paymentStatus: String,
    val transactionID: String,
)

// ---------- Merchant webhook (gateway → ticket-service) ----------

@JsonIgnoreProperties(ignoreUnknown = true)
data class MbwayWebhookPayload(
    val returnStatus: ReturnStatus? = null,
    val paymentStatus: String,
    val paymentMethod: String? = null,
    val transactionID: String,
    val amount: Amount? = null,
    val merchant: MerchantOut? = null,
    val paymentType: String? = null,
    val notificationID: String? = null,
)
