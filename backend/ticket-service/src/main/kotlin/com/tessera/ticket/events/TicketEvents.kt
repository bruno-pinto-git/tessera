package com.tessera.ticket.events

import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Event payloads for `ticket.ticket.paid` and `ticket.ticket.validated`.
 *
 * Contract spec: docs/events/async-contracts.md
 *
 * Both events are consumed by statistics-service to populate the
 * `ticket_sale` read-side table (insert on paid, update validatedAt on
 * validated).
 */
data class TicketPaidEvent(
    val version: Int = 1,
    val occurredAt: OffsetDateTime,
    val ticketId: Long,
    val eventId: Long,
    val matchId: Long?,
    /** Home club of the match, so statistics can aggregate sales per club. */
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
    /** Keycloak subject UUID of the staff/platform-admin user that validated the ticket. */
    val validatorSub: String?,
)
