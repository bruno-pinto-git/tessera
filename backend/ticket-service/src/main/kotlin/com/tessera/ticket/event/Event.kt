package com.tessera.ticket.event

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "event")
class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "match_id")
    val matchId: Long? = null,

    val name: String? = null,

    @Column(name = "price_normal", nullable = false)
    val priceNormal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "price_supporter", nullable = false)
    val priceSupporter: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    val status: String = "DRAFT",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
