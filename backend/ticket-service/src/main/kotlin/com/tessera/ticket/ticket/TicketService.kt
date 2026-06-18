package com.tessera.ticket.ticket

import com.tessera.ticket.event.Event
import com.tessera.ticket.event.EventRepository
import com.tessera.ticket.event.MatchAvailability
import com.tessera.ticket.event.MatchLookupClient
import com.tessera.ticket.events.TicketEventPublisher
import com.tessera.ticket.payments.MbwayGatewayClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class TicketService(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository,
    private val publisher: TicketEventPublisher,
    private val mbwayGateway: MbwayGatewayClient,
    private val matchLookup: MatchLookupClient,
) {

    @Transactional
    fun create(eventId: Long, supporter: Boolean, ownerSub: String): Ticket {
        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException("Event not found: $eventId") }

        assertSaleOpen(event)

        val price = if (supporter) event.priceSupporter else event.priceNormal

        val ticket = Ticket(
            event = event,
            price = price,
            ownerSub = ownerSub,
        )

        return ticketRepository.save(ticket)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): Ticket =
        ticketRepository.findById(id)
            .orElseThrow { TicketNotFoundException("Ticket not found: $id") }

    @Transactional(readOnly = true)
    fun findByOwner(ownerSub: String, pageable: Pageable): Page<Ticket> =
        ticketRepository.findByOwnerSub(ownerSub, pageable)

    @Transactional(readOnly = true)
    fun findByEvent(eventId: Long, pageable: Pageable): Page<Ticket> =
        ticketRepository.findByEventId(eventId, pageable)

    /**
     * Pays a PENDING ticket. Two flavours:
     *
     *  - **CASH / CARD** — synchronous: the ticket transitions PENDING → PAID
     *    immediately and `ticket.ticket.paid` is published on commit. There is
     *    no external gateway in the loop.
     *
     *  - **MBWAY** — asynchronous: we call [MbwayGatewayClient] to push a
     *    request to the customer's phone. The ticket stays PENDING (with
     *    `mbwayTransactionId` stamped). The transition to PAID happens later,
     *    in `MbwayWebhookService`, when the gateway calls our webhook.
     */
    @Transactional
    fun pay(id: Long, paymentMethod: String, phoneNumber: String?, mbwayReference: String?): Ticket {
        val ticket = ticketRepository.findById(id)
            .orElseThrow { TicketNotFoundException("Ticket not found: $id") }

        if (ticket.status != TicketStatus.PENDING) {
            throw InvalidTicketStatusException(
                "Cannot pay ticket in status ${ticket.status} (expected PENDING)"
            )
        }
        val method = paymentMethod.uppercase()
        if (method !in ALLOWED_PAYMENT_METHODS) {
            throw IllegalArgumentException(
                "Invalid payment method '$paymentMethod'; expected one of $ALLOWED_PAYMENT_METHODS"
            )
        }

        ticket.paymentMethod = method
        ticket.mbwayReference = mbwayReference

        return if (method == "MBWAY") {
            val phone = phoneNumber?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("phoneNumber is required for MBWAY payments")
            val transactionId = mbwayGateway.initiatePayment(ticket, phone)
            ticket.mbwayTransactionId = transactionId
            ticketRepository.save(ticket)
        } else {
            ticket.status = TicketStatus.PAID
            ticket.paymentDate = OffsetDateTime.now(ZoneOffset.UTC)
            val saved = ticketRepository.save(ticket)
            publishOnCommit { publisher.publishTicketPaid(saved) }
            saved
        }
    }

    /**
     * Transition PAID → VALIDATED at the gate. Identified by the Keycloak
     * subject UUID. Publishes `ticket.ticket.validated` once the transaction
     * commits.
     *
     * Authorization (beyond the controller's staff/admin role gate):
     *  - `platform-admin` may validate any ticket, any time.
     *  - otherwise the caller must be STAFF of the match's **home** club and
     *    act inside the activity window (2h before kickoff .. end of match); the
     *    match must not be CANCELLED.
     */
    @Transactional
    fun validate(
        code: UUID,
        validatorSub: String,
        isPlatformAdmin: Boolean,
        staffClubIds: Set<Long>,
    ): Ticket {
        val ticket = ticketRepository.findByCode(code)
            ?: throw TicketNotFoundException("Ticket not found: $code")

        if (!isPlatformAdmin) {
            authorizeStaffValidation(ticket, staffClubIds)
        }

        if (ticket.status != TicketStatus.PAID) {
            throw InvalidTicketStatusException(
                "Ticket status is ${ticket.status}, expected ${TicketStatus.PAID}"
            )
        }

        ticket.status = TicketStatus.VALIDATED
        ticket.validationDate = OffsetDateTime.now(ZoneOffset.UTC)
        ticket.validatorSub = validatorSub

        val saved = ticketRepository.save(ticket)
        publishOnCommit { publisher.publishTicketValidated(saved) }
        return saved
    }

    /**
     * A non-admin validator must be staff of the match's home club and act
     * within the activity window. Throws [AccessDeniedException] (→ 403) otherwise.
     */
    private fun authorizeStaffValidation(ticket: Ticket, staffClubIds: Set<Long>) {
        val matchId = ticket.event?.matchId
            ?: throw AccessDeniedException("Only platform admins can validate tickets not tied to a match.")
        val match = matchLookup.find(matchId)
            ?: throw AccessDeniedException("Could not resolve the match for this ticket.")

        val homeClubId = match.homeClubId
        if (homeClubId == null || homeClubId !in staffClubIds) {
            throw AccessDeniedException("You can only validate tickets for your own club's home matches.")
        }
        if (match.status == "CANCELLED") {
            throw AccessDeniedException("This match has been cancelled.")
        }

        val kickoff = match.kickoffAt?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
            ?: throw AccessDeniedException("This match has no usable kickoff time.")
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        if (now.isBefore(kickoff.minusHours(VALIDATION_OPENS_HOURS_BEFORE)) ||
            now.isAfter(kickoff.plusHours(MatchAvailability.MATCH_DURATION_HOURS))
        ) {
            throw AccessDeniedException(
                "Tickets can only be validated from ${VALIDATION_OPENS_HOURS_BEFORE}h before kickoff " +
                    "until the match ends.",
            )
        }
    }

    /**
     * Tickets can't be bought once the match is over or off. Match-less events
     * (no `matchId`) have no time limit. If the match can't be resolved (deleted
     * or match-service unreachable) we fail open and allow the sale — the
     * validation gate stays the strict guard.
     */
    private fun assertSaleOpen(event: Event) {
        val matchId = event.matchId ?: return
        val match = matchLookup.find(matchId) ?: return
        MatchAvailability.closedReason(match)?.let {
            throw SaleClosedException("Tickets are no longer on sale: $it.")
        }
    }

    private fun publishOnCommit(action: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() { action() }
                }
            )
        } else {
            action()
        }
    }

    companion object {
        val ALLOWED_PAYMENT_METHODS = setOf("MBWAY", "CARD", "CASH")

        /** Staff validation opens this many hours before kickoff (admins exempt). */
        const val VALIDATION_OPENS_HOURS_BEFORE = 2L
    }
}

class TicketNotFoundException(message: String) : RuntimeException(message)
class EventNotFoundException(message: String) : RuntimeException(message)
class InvalidTicketStatusException(message: String) : RuntimeException(message)
class SaleClosedException(message: String) : RuntimeException(message)
