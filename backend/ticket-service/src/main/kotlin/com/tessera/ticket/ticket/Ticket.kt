package com.tessera.ticket.ticket

import com.tessera.ticket.event.Event
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

enum class TicketStatus {
    PENDING,
    PAID,
    VALIDATED
}

@Entity
@Table(name = "ticket")
class Ticket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    val event: Event? = null,

    @Column(nullable = false, unique = true)
    val code: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val price: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TicketStatus = TicketStatus.PENDING,

    @Column(name = "payment_method")
    var paymentMethod: String? = null,

    @Column(name = "mbway_reference")
    var mbwayReference: String? = null,

    /** Keycloak subject UUID of the buyer. Null only for legacy rows. */
    @Column(name = "owner_sub")
    val ownerSub: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "payment_date")
    var paymentDate: OffsetDateTime? = null,

    @Column(name = "validation_date")
    var validationDate: OffsetDateTime? = null,

    /** Keycloak subject UUID of the staff member that validated the ticket. */
    @Column(name = "validator_sub")
    var validatorSub: String? = null,
)
