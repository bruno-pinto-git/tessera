package com.tessera.ticket.events

import java.math.BigDecimal
import java.time.OffsetDateTime

data class TicketPaidEvent(
    val version: Int = 1,
    val occurredAt: OffsetDateTime,
    val ticketId: Long,
    val eventId: Long,
    val matchId: Long?,
    val homeClubId: Long? = null,
    val price: BigDecimal,
    val paymentMethod: String?,
    val paidAt: OffsetDateTime,
)

data class TicketValidatedEvent(
    val version: Int = 1,
    val occurredAt: OffsetDateTime,
    val ticketId: Long,
    val matchId: Long?,
    val validatedAt: OffsetDateTime,
    val validatorSub: String?,
)
