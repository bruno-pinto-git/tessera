package com.tessera.ticket.events

import com.tessera.ticket.event.Event
import com.tessera.ticket.event.MatchLookupClient
import com.tessera.ticket.ticket.Ticket
import com.tessera.ticket.ticket.TicketStatus
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.math.BigDecimal
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [TicketEventPublisher] — focused on the `homeClubId` snapshot
 * carried by `ticket.ticket.paid`, which is what lets statistics-service
 * aggregate sales per club without calling back into match-service.
 */
class TicketEventPublisherTest {

    private val rabbit: RabbitTemplate = mock()
    private val matchLookup: MatchLookupClient = mock()
    private val publisher = TicketEventPublisher(rabbit, matchLookup, EXCHANGE)

    @Test
    fun `publishTicketPaid resolves and stamps the home club from the match`() {
        whenever(matchLookup.homeClubId(99L)).thenReturn(7L)

        publisher.publishTicketPaid(paidTicket(matchId = 99L))

        val ev = capturePaid()
        assertEquals(7L, ev.homeClubId)
        assertEquals(99L, ev.matchId)
        assertEquals(BigDecimal("12.00"), ev.price)
    }

    @Test
    fun `publishTicketPaid leaves home club null for a match-less event`() {
        publisher.publishTicketPaid(paidTicket(matchId = null))

        val ev = capturePaid()
        assertNull(ev.homeClubId)
        // No match → no lookup attempted.
        verify(matchLookup, never()).homeClubId(any())
    }

    private fun capturePaid(): TicketPaidEvent {
        val captor = argumentCaptor<TicketPaidEvent>()
        verify(rabbit).convertAndSend(
            eq(EXCHANGE),
            eq(TicketEventPublisher.ROUTING_PAID),
            captor.capture(),
        )
        return captor.firstValue
    }

    private fun paidTicket(matchId: Long?) = Ticket(
        id = 1L,
        event = Event(id = 2L, matchId = matchId, name = "Bilheteira"),
        price = BigDecimal("12.00"),
        status = TicketStatus.PAID,
        paymentMethod = "CARD",
        paymentDate = OffsetDateTime.parse("2026-05-01T19:00:00Z"),
    )

    companion object {
        private const val EXCHANGE = "tessera.events"
    }
}
