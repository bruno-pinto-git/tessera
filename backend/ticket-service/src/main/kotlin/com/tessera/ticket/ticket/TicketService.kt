package com.tessera.ticket.ticket

import com.tessera.ticket.event.Event
import com.tessera.ticket.event.EventRepository
import com.tessera.ticket.event.MatchAvailability
import com.tessera.ticket.event.MatchLookupClient
import com.tessera.ticket.events.TicketEventPublisher
import com.tessera.ticket.payments.MbwayGatewayClient
import com.tessera.ticket.payments.StripeGatewayClient
import org.slf4j.LoggerFactory
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
    private val stripeGateway: StripeGatewayClient,
    private val matchLookup: MatchLookupClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

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

    /**
     * Like [getById], but for a PENDING CARD ticket also polls Stripe's
     * Checkout Session status first. There is no reachable webhook endpoint,
     * so this is the confirmation path — triggered whenever a client reads
     * the ticket back (e.g. after returning from Stripe's hosted page). A
     * Stripe hiccup here just leaves the ticket PENDING for this read; the
     * caller's next read retries. Not applied to list endpoints (`mine`,
     * `findByEvent`) to avoid an N+1 Stripe call per row.
     */
    fun getByIdRefreshed(id: Long): Ticket {
        val ticket = getById(id)
        if (ticket.status != TicketStatus.PENDING || ticket.paymentMethod != "CARD") return ticket
        val sessionId = ticket.stripeCheckoutSessionId ?: return ticket
        try {
            if (stripeGateway.checkStatus(sessionId) == "paid") markPaid(ticket)
        } catch (e: Exception) {
            log.warn("Stripe: status check failed for ticket id={}: {}", ticket.id, e.message)
        }
        return ticket
    }

    @Transactional(readOnly = true)
    fun findByOwner(ownerSub: String, pageable: Pageable): Page<Ticket> =
        ticketRepository.findByOwnerSub(ownerSub, pageable)

    @Transactional(readOnly = true)
    fun findByEvent(eventId: Long, pageable: Pageable): Page<Ticket> =
        ticketRepository.findByEventId(eventId, pageable)

    data class PayResult(val ticket: Ticket, val checkoutUrl: String? = null)

    /**
     * Pays a PENDING ticket. Two flavours, both asynchronous:
     *
     *  - **MBWAY** — we call [MbwayGatewayClient] to push a request to the
     *    customer's phone. The ticket stays PENDING (with
     *    `mbwayTransactionId` stamped). The transition to PAID happens later,
     *    in `MbwayWebhookService`, when the gateway calls our webhook.
     *
     *  - **CARD** — we create a Stripe Checkout Session and return its
     *    hosted `checkoutUrl` for the caller to send the buyer to. The
     *    ticket stays PENDING (with `stripeCheckoutSessionId` stamped) until
     *    [getByIdRefreshed] observes the session as paid.
     */
    @Transactional
    fun pay(id: Long, paymentMethod: String, phoneNumber: String?, mbwayReference: String?): PayResult {
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

        return when (method) {
            "MBWAY" -> {
                val phone = phoneNumber?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("phoneNumber is required for MBWAY payments")
                val transactionId = mbwayGateway.initiatePayment(ticket, phone)
                ticket.mbwayTransactionId = transactionId
                PayResult(ticketRepository.save(ticket))
            }
            "CARD" -> {
                val initiation = stripeGateway.createCheckoutSession(ticket)
                ticket.stripeCheckoutSessionId = initiation.sessionId
                PayResult(ticketRepository.save(ticket), checkoutUrl = initiation.checkoutUrl)
            }
            else -> error("unreachable: '$method' already validated against $ALLOWED_PAYMENT_METHODS")
        }
    }

    private fun markPaid(ticket: Ticket) {
        ticket.status = TicketStatus.PAID
        ticket.paymentDate = OffsetDateTime.now(ZoneOffset.UTC)
        val saved = ticketRepository.save(ticket)
        publishOnCommit { publisher.publishTicketPaid(saved) }
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
        val ALLOWED_PAYMENT_METHODS = setOf("MBWAY", "CARD")

        /** Staff validation opens this many hours before kickoff (admins exempt). */
        const val VALIDATION_OPENS_HOURS_BEFORE = 2L
    }
}

class TicketNotFoundException(message: String) : RuntimeException(message)
class EventNotFoundException(message: String) : RuntimeException(message)
class InvalidTicketStatusException(message: String) : RuntimeException(message)
class SaleClosedException(message: String) : RuntimeException(message)
