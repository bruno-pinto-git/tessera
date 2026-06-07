package com.tessera.ticket.ticket

import com.tessera.ticket.event.Event
import com.tessera.ticket.event.EventRepository
import com.tessera.ticket.events.TicketEventPublisher
import com.tessera.ticket.payments.MbwayGatewayClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Unit tests for [TicketService] — the ticket status machine and the
 * synchronous (CARD/CASH) vs asynchronous (MBWAY) payment flows, mirroring
 * docs/http-tests/09-tickets.http. Collaborators are mocked; the post-commit
 * publish runs inline because there is no active transaction in a unit test.
 */
class TicketServiceTest {

    private val ticketRepository: TicketRepository = mock()
    private val eventRepository: EventRepository = mock()
    private val publisher: TicketEventPublisher = mock()
    private val mbwayGateway: MbwayGatewayClient = mock()

    private val service = TicketService(ticketRepository, eventRepository, publisher, mbwayGateway)

    // ----- create -------------------------------------------------------------

    @Test
    fun `create fails when the event does not exist`() {
        whenever(eventRepository.findById(1L)).thenReturn(Optional.empty())
        assertFailsWith<EventNotFoundException> { service.create(1L, supporter = false, ownerSub = "u") }
    }

    @Test
    fun `create uses the normal price for a non-supporter`() {
        whenever(eventRepository.findById(1L)).thenReturn(Optional.of(event()))
        val captor = argumentCaptor<Ticket>()
        doReturn(ticket()).whenever(ticketRepository).save(captor.capture())

        service.create(1L, supporter = false, ownerSub = "u")

        assertEquals(BigDecimal("10.00"), captor.firstValue.price)
        assertEquals(TicketStatus.PENDING, captor.firstValue.status)
    }

    @Test
    fun `create uses the supporter price for a supporter`() {
        whenever(eventRepository.findById(1L)).thenReturn(Optional.of(event()))
        val captor = argumentCaptor<Ticket>()
        doReturn(ticket()).whenever(ticketRepository).save(captor.capture())

        service.create(1L, supporter = true, ownerSub = "u")

        assertEquals(BigDecimal("5.00"), captor.firstValue.price)
    }

    // ----- pay ----------------------------------------------------------------

    @Test
    fun `pay fails for an unknown ticket`() {
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.empty())
        assertFailsWith<TicketNotFoundException> { service.pay(7L, "CARD", null, null) }
    }

    @Test
    fun `pay rejects a ticket that is not pending`() {
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket(status = TicketStatus.PAID)))
        assertFailsWith<InvalidTicketStatusException> { service.pay(7L, "CARD", null, null) }
    }

    @Test
    fun `pay rejects an unknown payment method`() {
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket()))
        assertFailsWith<IllegalArgumentException> { service.pay(7L, "BITCOIN", null, null) }
    }

    @Test
    fun `paying by card transitions to PAID and publishes`() {
        val t = ticket()
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(t))
        doReturn(t).whenever(ticketRepository).save(any())

        val paid = service.pay(7L, "card", null, null)

        assertEquals(TicketStatus.PAID, paid.status)
        assertEquals("CARD", paid.paymentMethod)
        verify(publisher).publishTicketPaid(paid)
        verify(mbwayGateway, never()).initiatePayment(any(), any())
    }

    @Test
    fun `paying by mbway stays pending and stamps the transaction id`() {
        val t = ticket()
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(t))
        whenever(mbwayGateway.initiatePayment(eq(t), eq("912345678"))).thenReturn("txn-123")
        doReturn(t).whenever(ticketRepository).save(any())

        val pending = service.pay(7L, "MBWAY", "912345678", "ref-1")

        assertEquals(TicketStatus.PENDING, pending.status)
        assertEquals("txn-123", pending.mbwayTransactionId)
        verify(mbwayGateway).initiatePayment(t, "912345678")
        verify(publisher, never()).publishTicketPaid(any())
    }

    @Test
    fun `paying by mbway without a phone number is rejected`() {
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket()))
        assertFailsWith<IllegalArgumentException> { service.pay(7L, "MBWAY", "  ", null) }
    }

    // ----- validate -----------------------------------------------------------

    @Test
    fun `validate fails for an unknown code`() {
        val code = UUID.randomUUID()
        whenever(ticketRepository.findByCode(code)).thenReturn(null)
        assertFailsWith<TicketNotFoundException> { service.validate(code, "staff-sub") }
    }

    @Test
    fun `validate rejects a ticket that is not paid`() {
        val code = UUID.randomUUID()
        whenever(ticketRepository.findByCode(code)).thenReturn(ticket(status = TicketStatus.PENDING))
        assertFailsWith<InvalidTicketStatusException> { service.validate(code, "staff-sub") }
    }

    @Test
    fun `validate transitions a paid ticket and publishes`() {
        val code = UUID.randomUUID()
        val t = ticket(status = TicketStatus.PAID)
        whenever(ticketRepository.findByCode(code)).thenReturn(t)
        doReturn(t).whenever(ticketRepository).save(any())

        val validated = service.validate(code, "staff-sub")

        assertEquals(TicketStatus.VALIDATED, validated.status)
        assertEquals("staff-sub", validated.validatorSub)
        verify(publisher).publishTicketValidated(validated)
    }

    @Test
    fun `paying by mbway never publishes a paid event up front`() {
        val t = ticket()
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(t))
        whenever(mbwayGateway.initiatePayment(any(), any())).thenReturn("txn-9")
        doReturn(t).whenever(ticketRepository).save(any())

        service.pay(7L, "MBWAY", "912000000", null)

        assertNull(t.paymentDate)
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private fun event() = Event(
        id = 1L,
        matchId = 99L,
        name = "Demo",
        priceNormal = BigDecimal("10.00"),
        priceSupporter = BigDecimal("5.00"),
        status = "PUBLISHED",
    )

    private fun ticket(status: TicketStatus = TicketStatus.PENDING) = Ticket(
        id = 7L,
        event = event(),
        price = BigDecimal("10.00"),
        status = status,
        ownerSub = "owner-sub",
    )
}
