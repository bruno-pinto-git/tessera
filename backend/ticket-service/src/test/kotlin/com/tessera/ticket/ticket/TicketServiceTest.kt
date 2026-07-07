package com.tessera.ticket.ticket

import com.tessera.ticket.event.Event
import com.tessera.ticket.event.EventRepository
import com.tessera.ticket.event.MatchLookupClient
import com.tessera.ticket.events.TicketEventPublisher
import com.tessera.ticket.payments.MbwayGatewayClient
import com.tessera.ticket.payments.StripeGatewayClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TicketServiceTest {

    private val ticketRepository: TicketRepository = mock()
    private val eventRepository: EventRepository = mock()
    private val publisher: TicketEventPublisher = mock()
    private val mbwayGateway: MbwayGatewayClient = mock()
    private val stripeGateway: StripeGatewayClient = mock()
    private val matchLookup: MatchLookupClient = mock()

    private val service =
        TicketService(ticketRepository, eventRepository, publisher, mbwayGateway, stripeGateway, matchLookup)

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

    @Test
    fun `create is blocked once the match has ended`() {
        whenever(eventRepository.findById(1L)).thenReturn(Optional.of(event()))
        whenever(matchLookup.find(99L))
            .thenReturn(matchView(homeClubId = 5L, kickoff = OffsetDateTime.now().minusHours(5)))
        assertFailsWith<SaleClosedException> { service.create(1L, supporter = false, ownerSub = "u") }
        verify(ticketRepository, never()).save(any())
    }

    @Test
    fun `create is blocked for a cancelled match`() {
        whenever(eventRepository.findById(1L)).thenReturn(Optional.of(event()))
        whenever(matchLookup.find(99L)).thenReturn(
            matchView(homeClubId = 5L, kickoff = OffsetDateTime.now().plusHours(1), status = "CANCELLED"),
        )
        assertFailsWith<SaleClosedException> { service.create(1L, supporter = false, ownerSub = "u") }
    }

    @Test
    fun `create succeeds for an upcoming match`() {
        whenever(eventRepository.findById(1L)).thenReturn(Optional.of(event()))
        whenever(matchLookup.find(99L))
            .thenReturn(matchView(homeClubId = 5L, kickoff = OffsetDateTime.now().plusHours(2)))
        doReturn(ticket()).whenever(ticketRepository).save(any())

        val t = service.create(1L, supporter = false, ownerSub = "u")

        assertEquals(TicketStatus.PENDING, t.status)
    }

    @Test
    fun `create succeeds for a match-less event without touching match-service`() {
        val e = Event(
            id = 2L, matchId = null, name = "Avulso",
            priceNormal = BigDecimal("10.00"), priceSupporter = BigDecimal("5.00"), status = "PUBLISHED",
        )
        whenever(eventRepository.findById(2L)).thenReturn(Optional.of(e))
        doReturn(ticket()).whenever(ticketRepository).save(any())

        service.create(2L, supporter = false, ownerSub = "u")

        verify(matchLookup, never()).find(any())
    }

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
    fun `paying by card creates a Stripe checkout session and stays pending`() {
        val t = ticket()
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(t))
        whenever(stripeGateway.createCheckoutSession(t))
            .thenReturn(StripeGatewayClient.StripeCheckoutInitiation("cs_123", "https://checkout.stripe.com/pay/cs_123"))
        doReturn(t).whenever(ticketRepository).save(any())

        val result = service.pay(7L, "card", null, null)

        assertEquals(TicketStatus.PENDING, result.ticket.status)
        assertEquals("CARD", result.ticket.paymentMethod)
        assertEquals("cs_123", result.ticket.stripeCheckoutSessionId)
        assertEquals("https://checkout.stripe.com/pay/cs_123", result.checkoutUrl)
        verify(publisher, never()).publishTicketPaid(any())
        verify(mbwayGateway, never()).initiatePayment(any(), any())
    }

    @Test
    fun `paying by mbway stays pending and stamps the transaction id`() {
        val t = ticket()
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(t))
        whenever(mbwayGateway.initiatePayment(eq(t), eq("912345678"))).thenReturn("txn-123")
        doReturn(t).whenever(ticketRepository).save(any())

        val result = service.pay(7L, "MBWAY", "912345678", "ref-1")

        assertEquals(TicketStatus.PENDING, result.ticket.status)
        assertEquals("txn-123", result.ticket.mbwayTransactionId)
        assertNull(result.checkoutUrl)
        verify(mbwayGateway).initiatePayment(t, "912345678")
        verify(publisher, never()).publishTicketPaid(any())
    }

    @Test
    fun `paying by mbway without a phone number is rejected`() {
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket()))
        assertFailsWith<IllegalArgumentException> { service.pay(7L, "MBWAY", "  ", null) }
    }

    @Test
    fun `validate fails for an unknown code`() {
        val code = UUID.randomUUID()
        whenever(ticketRepository.findByCode(code)).thenReturn(null)
        assertFailsWith<TicketNotFoundException> { service.validate(code, "staff-sub", isPlatformAdmin = true, staffClubIds = emptySet()) }
    }

    @Test
    fun `validate rejects a ticket that is not paid`() {
        val code = UUID.randomUUID()
        whenever(ticketRepository.findByCode(code)).thenReturn(ticket(status = TicketStatus.PENDING))
        assertFailsWith<InvalidTicketStatusException> { service.validate(code, "staff-sub", isPlatformAdmin = true, staffClubIds = emptySet()) }
    }

    @Test
    fun `validate transitions a paid ticket and publishes`() {
        val code = UUID.randomUUID()
        val t = ticket(status = TicketStatus.PAID)
        whenever(ticketRepository.findByCode(code)).thenReturn(t)
        doReturn(t).whenever(ticketRepository).save(any())

        val validated = service.validate(code, "staff-sub", isPlatformAdmin = true, staffClubIds = emptySet())

        assertEquals(TicketStatus.VALIDATED, validated.status)
        assertEquals("staff-sub", validated.validatorSub)
        verify(publisher).publishTicketValidated(validated)
    }

    @Test
    fun `staff of the home club can validate within the window`() {
        val code = UUID.randomUUID()
        val t = ticket(status = TicketStatus.PAID)
        whenever(ticketRepository.findByCode(code)).thenReturn(t)
        whenever(matchLookup.find(99L)).thenReturn(matchView(homeClubId = 5L, kickoff = OffsetDateTime.now().plusHours(1)))
        doReturn(t).whenever(ticketRepository).save(any())

        val v = service.validate(code, "staff-sub", isPlatformAdmin = false, staffClubIds = setOf(5L))

        assertEquals(TicketStatus.VALIDATED, v.status)
        verify(publisher).publishTicketValidated(v)
    }

    @Test
    fun `staff of another club cannot validate`() {
        val code = UUID.randomUUID()
        whenever(ticketRepository.findByCode(code)).thenReturn(ticket(status = TicketStatus.PAID))
        whenever(matchLookup.find(99L)).thenReturn(matchView(homeClubId = 5L, kickoff = OffsetDateTime.now().plusHours(1)))
        assertFailsWith<AccessDeniedException> {
            service.validate(code, "staff-sub", isPlatformAdmin = false, staffClubIds = setOf(7L))
        }
    }

    @Test
    fun `validation before the window opens is denied`() {
        val code = UUID.randomUUID()
        whenever(ticketRepository.findByCode(code)).thenReturn(ticket(status = TicketStatus.PAID))
        whenever(matchLookup.find(99L)).thenReturn(matchView(homeClubId = 5L, kickoff = OffsetDateTime.now().plusHours(5)))
        assertFailsWith<AccessDeniedException> {
            service.validate(code, "staff-sub", isPlatformAdmin = false, staffClubIds = setOf(5L))
        }
    }

    @Test
    fun `validation after the window closes is denied`() {
        val code = UUID.randomUUID()
        whenever(ticketRepository.findByCode(code)).thenReturn(ticket(status = TicketStatus.PAID))
        whenever(matchLookup.find(99L)).thenReturn(matchView(homeClubId = 5L, kickoff = OffsetDateTime.now().minusHours(5)))
        assertFailsWith<AccessDeniedException> {
            service.validate(code, "staff-sub", isPlatformAdmin = false, staffClubIds = setOf(5L))
        }
    }

    @Test
    fun `validation more than 2h before kickoff is denied`() {
        val code = UUID.randomUUID()
        whenever(ticketRepository.findByCode(code)).thenReturn(ticket(status = TicketStatus.PAID))
        whenever(matchLookup.find(99L))
            .thenReturn(matchView(homeClubId = 5L, kickoff = OffsetDateTime.now().plusMinutes(150)))
        assertFailsWith<AccessDeniedException> {
            service.validate(code, "staff-sub", isPlatformAdmin = false, staffClubIds = setOf(5L))
        }
    }

    @Test
    fun `validation after the match has ended is denied`() {
        val code = UUID.randomUUID()
        whenever(ticketRepository.findByCode(code)).thenReturn(ticket(status = TicketStatus.PAID))
        whenever(matchLookup.find(99L))
            .thenReturn(matchView(homeClubId = 5L, kickoff = OffsetDateTime.now().minusMinutes(150)))
        assertFailsWith<AccessDeniedException> {
            service.validate(code, "staff-sub", isPlatformAdmin = false, staffClubIds = setOf(5L))
        }
    }

    @Test
    fun `validation of a cancelled match is denied`() {
        val code = UUID.randomUUID()
        whenever(ticketRepository.findByCode(code)).thenReturn(ticket(status = TicketStatus.PAID))
        whenever(matchLookup.find(99L))
            .thenReturn(matchView(homeClubId = 5L, kickoff = OffsetDateTime.now().plusHours(1), status = "CANCELLED"))
        assertFailsWith<AccessDeniedException> {
            service.validate(code, "staff-sub", isPlatformAdmin = false, staffClubIds = setOf(5L))
        }
    }

    @Test
    fun `a platform-admin can validate outside the window and any club`() {
        val code = UUID.randomUUID()
        val t = ticket(status = TicketStatus.PAID)
        whenever(ticketRepository.findByCode(code)).thenReturn(t)
        doReturn(t).whenever(ticketRepository).save(any())

        val v = service.validate(code, "admin-sub", isPlatformAdmin = true, staffClubIds = emptySet())

        assertEquals(TicketStatus.VALIDATED, v.status)
        verify(matchLookup, never()).find(any())
    }

    @Test
    fun `staff cannot validate a ticket with no match`() {
        val code = UUID.randomUUID()
        val noMatch = Ticket(
            id = 8L,
            event = Event(id = 2L, matchId = null, name = "Avulso"),
            price = BigDecimal("10.00"),
            status = TicketStatus.PAID,
            ownerSub = "owner-sub",
        )
        whenever(ticketRepository.findByCode(code)).thenReturn(noMatch)
        assertFailsWith<AccessDeniedException> {
            service.validate(code, "staff-sub", isPlatformAdmin = false, staffClubIds = setOf(5L))
        }
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

    @Test
    fun `getByIdRefreshed does not call Stripe for a non-pending ticket`() {
        whenever(ticketRepository.findById(7L)).thenReturn(
            Optional.of(ticket(status = TicketStatus.PAID, paymentMethod = "CARD", stripeCheckoutSessionId = "cs_1")),
        )

        service.getByIdRefreshed(7L)

        verify(stripeGateway, never()).checkStatus(any())
    }

    @Test
    fun `getByIdRefreshed does not call Stripe for a pending non-card ticket`() {
        whenever(ticketRepository.findById(7L)).thenReturn(
            Optional.of(ticket(status = TicketStatus.PENDING, paymentMethod = "MBWAY")),
        )

        service.getByIdRefreshed(7L)

        verify(stripeGateway, never()).checkStatus(any())
    }

    @Test
    fun `getByIdRefreshed marks the ticket PAID when Stripe reports paid`() {
        val t = ticket(paymentMethod = "CARD", stripeCheckoutSessionId = "cs_1")
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(t))
        whenever(stripeGateway.checkStatus("cs_1")).thenReturn("paid")
        doReturn(t).whenever(ticketRepository).save(any())

        val refreshed = service.getByIdRefreshed(7L)

        assertEquals(TicketStatus.PAID, refreshed.status)
        verify(publisher).publishTicketPaid(t)
    }

    @Test
    fun `getByIdRefreshed leaves the ticket pending when Stripe reports unpaid`() {
        val t = ticket(paymentMethod = "CARD", stripeCheckoutSessionId = "cs_1")
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(t))
        whenever(stripeGateway.checkStatus("cs_1")).thenReturn("unpaid")

        val refreshed = service.getByIdRefreshed(7L)

        assertEquals(TicketStatus.PENDING, refreshed.status)
        verify(ticketRepository, never()).save(any())
    }

    @Test
    fun `getByIdRefreshed swallows a Stripe failure and returns the ticket unchanged`() {
        val t = ticket(paymentMethod = "CARD", stripeCheckoutSessionId = "cs_1")
        whenever(ticketRepository.findById(7L)).thenReturn(Optional.of(t))
        whenever(stripeGateway.checkStatus("cs_1")).thenThrow(RuntimeException("Stripe is down"))

        val refreshed = service.getByIdRefreshed(7L)

        assertEquals(TicketStatus.PENDING, refreshed.status)
    }

    private fun event() = Event(
        id = 1L,
        matchId = 99L,
        name = "Demo",
        priceNormal = BigDecimal("10.00"),
        priceSupporter = BigDecimal("5.00"),
        status = "PUBLISHED",
    )

    private fun ticket(
        status: TicketStatus = TicketStatus.PENDING,
        paymentMethod: String? = null,
        stripeCheckoutSessionId: String? = null,
    ) = Ticket(
        id = 7L,
        event = event(),
        price = BigDecimal("10.00"),
        status = status,
        paymentMethod = paymentMethod,
        stripeCheckoutSessionId = stripeCheckoutSessionId,
        ownerSub = "owner-sub",
    )

    private fun matchView(homeClubId: Long, kickoff: OffsetDateTime, status: String = "SCHEDULED") =
        MatchLookupClient.MatchView(
            id = 99L,
            homeClubId = homeClubId,
            kickoffAt = kickoff.toString(),
            status = status,
        )
}
