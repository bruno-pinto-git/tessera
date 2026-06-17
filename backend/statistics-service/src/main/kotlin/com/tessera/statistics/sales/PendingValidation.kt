package com.tessera.statistics.sales

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

/**
 * A `ticket.ticket.validated` that arrived before its `ticket.ticket.paid`.
 * Parked here until the paid event creates the sale, at which point the
 * validation timestamp is drained onto it. Avoids permanently losing a
 * validation on out-of-order delivery.
 */
@Entity
@Table(name = "pending_validation")
class PendingValidation(
    @Id
    @Column(name = "ticket_id")
    val ticketId: Long,

    @Column(name = "validated_at", nullable = false)
    val validatedAt: OffsetDateTime,
)

interface PendingValidationRepository : JpaRepository<PendingValidation, Long>
