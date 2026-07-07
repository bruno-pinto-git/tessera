package com.tessera.mockmbway.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class Amount(
    val value: Double,
    val currency: String,
)

@Serializable
data class ReturnStatus(
    val statusCode: String,
    val statusMsg: String,
    val statusDescription: String? = null,
)

@Serializable
data class MerchantOut(
    val terminalId: Int? = null,
    val channel: String? = null,
    val merchantTransactionId: String,
)

// ----- POST {backend}/api/v1/mbway/relay/poll (response) -----

@Serializable
data class RelayItem(
    val transactionID: String,
    val transactionSignature: String,
    val merchantTransactionId: String,
    val terminalId: Int? = null,
    val description: String? = null,
    val amount: Amount,
    val customerPhone: String,
    val callbackUrl: String? = null,
)

// ----- Merchant webhook (server → ticket-service) -----

@Serializable
data class WebhookNotification(
    val returnStatus: ReturnStatus,
    val paymentStatus: String,
    val paymentMethod: String,
    val transactionID: String,
    val amount: Amount,
    val merchant: MerchantOut,
    val paymentType: String,
    val notificationID: String,
)
