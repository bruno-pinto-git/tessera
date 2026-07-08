package com.tessera.ticket.payments

import com.tessera.ticket.event.Event
import com.tessera.ticket.events.TicketEventPublisher
import com.tessera.ticket.ticket.Ticket
import com.tessera.ticket.ticket.TicketRepository
import com.tessera.ticket.ticket.TicketStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.assertEquals

class MbwayWebhookServiceTest {

    private val ticketRepository: TicketRepository = mock()
    private val publisher: TicketEventPublisher = mock()

    private val service = MbwayWebhookService(ticketRepository, publisher)

    @Test
    fun `unknown transaction id is a safe no-op`() {
        whenever(ticketRepository.findByMbwayTransactionId("nope")).thenReturn(null)

        service.handle(payload("Success", "nope"))

        verify(ticketRepository, never()).save(any())
        verify(publisher, never()).publishTicketPaid(any())
    }

    @Test
    fun `success marks a pending ticket paid and publishes`() {
        val t = ticket(TicketStatus.PENDING)
        whenever(ticketRepository.findByMbwayTransactionId("txn-1")).thenReturn(t)
        doReturn(t).whenever(ticketRepository).save(any())

        service.handle(payload("Success", "txn-1"))

        assertEquals(TicketStatus.PAID, t.status)
        verify(publisher).publishTicketPaid(t)
    }

    @Test
    fun `declined keeps the ticket pending`() {
        val t = ticket(TicketStatus.PENDING)
        whenever(ticketRepository.findByMbwayTransactionId("txn-1")).thenReturn(t)

        service.handle(payload("Declined", "txn-1"))

        assertEquals(TicketStatus.PENDING, t.status)
        verify(ticketRepository, never()).save(any())
        verify(publisher, never()).publishTicketPaid(any())
    }

    @Test
    fun `expired keeps the ticket pending`() {
        val t = ticket(TicketStatus.PENDING)
        whenever(ticketRepository.findByMbwayTransactionId("txn-1")).thenReturn(t)

        service.handle(payload("Expired", "txn-1"))

        assertEquals(TicketStatus.PENDING, t.status)
        verify(publisher, never()).publishTicketPaid(any())
    }

    @Test
    fun `a success callback for an already paid ticket is idempotent`() {
        val t = ticket(TicketStatus.PAID)
        whenever(ticketRepository.findByMbwayTransactionId("txn-1")).thenReturn(t)

        service.handle(payload("Success", "txn-1"))

        verify(ticketRepository, never()).save(any())
        verify(publisher, never()).publishTicketPaid(any())
    }

    @Test
    fun `an unknown payment status is ignored`() {
        val t = ticket(TicketStatus.PENDING)
        whenever(ticketRepository.findByMbwayTransactionId("txn-1")).thenReturn(t)

        service.handle(payload("Weird", "txn-1"))

        assertEquals(TicketStatus.PENDING, t.status)
        verify(ticketRepository, never()).save(any())
    }

    private fun payload(status: String, txn: String) =
        MbwayWebhookPayload(paymentStatus = status, transactionID = txn)

    private fun ticket(status: TicketStatus) = Ticket(
        id = 7L,
        event = Event(id = 1L, matchId = 99L, name = "Demo"),
        price = BigDecimal("10.00"),
        status = status,
        ownerSub = "owner",
        mbwayTransactionId = "txn-1",
    )
}
