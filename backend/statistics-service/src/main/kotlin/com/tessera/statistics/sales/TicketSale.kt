package com.tessera.statistics.sales

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "ticket_sale")
class TicketSale(
    @Id
    @Column(name = "ticket_id")
    val ticketId: Long,

    @Column(name = "event_id", nullable = false)
    val eventId: Long,

    @Column(name = "match_id", nullable = false)
    val matchId: Long,

    @Column(nullable = false)
    val price: BigDecimal,

    @Column(name = "payment_method", length = 20)
    val paymentMethod: String?,

    @Column(name = "paid_at", nullable = false)
    val paidAt: OffsetDateTime,

    @Column(name = "validated_at")
    var validatedAt: OffsetDateTime? = null,
)
