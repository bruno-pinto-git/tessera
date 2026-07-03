package com.tessera.android.data.dto

data class EventDto(
    val id: Long,
    val name: String?,
    val matchId: Long?,
    val priceNormal: Double,
    val priceSupporter: Double,
    val status: String,
    val matchLabel: String? = null,
)

data class TicketDto(
    val id: Long,
    val code: String,
    val eventId: Long,
    val matchId: Long?,
    val price: Double,
    val status: String,
    val paymentMethod: String?,
    val paymentDate: String?,
    val validationDate: String?,
    /** Stripe Checkout hosted-page URL. Only present in the pay() response for a fresh CARD payment. */
    val checkoutUrl: String? = null,
)
