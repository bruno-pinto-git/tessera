package com.tessera.ticket.payments

import com.tessera.ticket.events.TicketEventPublisher
import com.tessera.ticket.ticket.Ticket
import com.tessera.ticket.ticket.TicketRepository
import com.tessera.ticket.ticket.TicketStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class MbwayWebhookService(
    private val ticketRepository: TicketRepository,
    private val publisher: TicketEventPublisher,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun handle(payload: MbwayWebhookPayload) {
        val ticket = ticketRepository.findByMbwayTransactionId(payload.transactionID)
        if (ticket == null) {
            log.warn(
                "MB WAY webhook: no ticket matches transactionID={} — dropping",
                payload.transactionID,
            )
            return
        }
        when (payload.paymentStatus.uppercase()) {
            "SUCCESS" -> markPaid(ticket)
            "DECLINED", "EXPIRED" -> log.info(
                "MB WAY webhook: ticket id={} status={} — keeping PENDING",
                ticket.id,
                payload.paymentStatus,
            )
            else -> log.warn(
                "MB WAY webhook: unknown paymentStatus={} for ticket id={}",
                payload.paymentStatus,
                ticket.id,
            )
        }
    }

    private fun markPaid(ticket: Ticket) {
        if (ticket.status != TicketStatus.PENDING) {
            log.info(
                "MB WAY webhook: ticket id={} already in status={}, no-op",
                ticket.id,
                ticket.status,
            )
            return
        }
        ticket.status = TicketStatus.PAID
        ticket.paymentDate = OffsetDateTime.now(ZoneOffset.UTC)
        val saved = ticketRepository.save(ticket)
        publishOnCommit { publisher.publishTicketPaid(saved) }
        log.info("MB WAY webhook: ticket id={} → PAID", saved.id)
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
}
