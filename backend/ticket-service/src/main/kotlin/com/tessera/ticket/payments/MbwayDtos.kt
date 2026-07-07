package com.tessera.ticket.payments

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class Amount(
    val value: Double,
    val currency: String,
)

data class ReturnStatus(
    val statusCode: String,
    val statusMsg: String,
    val statusDescription: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MerchantOut(
    val terminalId: Int? = null,
    val channel: String? = null,
    val merchantTransactionId: String,
)

/** A pending MBWAY payment waiting for mock-mbway to pick it up via polling. */
data class MbwayRelayRequest(
    val transactionID: String,
    val transactionSignature: String,
    val merchantTransactionId: String,
    val terminalId: Int?,
    val description: String?,
    val amount: Amount,
    val customerPhone: String,
    val callbackUrl: String?,
)

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
