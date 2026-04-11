package com.tessera.ticket

import com.tessera.event.Event
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "ticket")
class Ticket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    val event: Event,

    @Column(nullable = false, unique = true)
    val code: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val price: BigDecimal,

    @Column(nullable = false)
    val status: String = "PENDING",

    @Column(name = "payment_method")
    val paymentMethod: String? = null,

    @Column(name = "mbway_reference")
    val mbwayReference: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "payment_date")
    val paymentDate: OffsetDateTime? = null,

    @Column(name = "validation_date")
    val validationDate: OffsetDateTime? = null,

    @Column(name = "validator_id")
    val validatorId: Long? = null
)
