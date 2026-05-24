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

// ----- POST /api/v1/payments -----

@Serializable
data class MerchantIn(
    val terminalId: Int? = null,
    val channel: String? = null,
    val merchantTransactionId: String,
    val transactionDescription: String? = null,
    val shopURL: String? = null,
    val callbackUrl: String? = null,
)

@Serializable
data class TransactionIn(
    val transactionTimestamp: String? = null,
    val description: String? = null,
    val moto: Boolean? = null,
    val paymentType: String,
    val paymentMethod: List<String>,
    val amount: Amount,
)

@Serializable
data class CreatePaymentRequest(
    val merchant: MerchantIn,
    val transaction: TransactionIn,
)

@Serializable
data class MerchantOut(
    val terminalId: Int? = null,
    val channel: String? = null,
    val merchantTransactionId: String,
)

@Serializable
data class CreatePaymentResponse(
    val returnStatus: ReturnStatus,
    val transactionID: String,
    val transactionSignature: String,
    val amount: Amount,
    val merchant: MerchantOut,
    val paymentMethodList: List<String>,
    val expiry: String,
)

// ----- POST /api/v1/payments/{id}/mbway/purchase -----

@Serializable
data class MbwayPurchaseRequest(
    val customerPhone: String,
)

@Serializable
data class MbwayPurchaseResponse(
    val returnStatus: ReturnStatus,
    val paymentStatus: String,
    val transactionID: String,
)

// ----- GET /api/v1/payments/{id}/status -----

@Serializable
data class StatusResponse(
    val returnStatus: ReturnStatus,
    val paymentStatus: String,
    val transactionID: String,
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
