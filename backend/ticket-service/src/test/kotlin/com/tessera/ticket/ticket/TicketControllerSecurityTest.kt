package com.tessera.ticket.ticket

import com.tessera.ticket.config.SecurityConfig
import com.tessera.ticket.event.Event
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.mockito.kotlin.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.UUID

/**
 * RBAC web tests for [TicketController] — the real Spring Security filter chain
 * + method security, plus the in-code owner-or-staff check on pay/get. Mirrors
 * docs/http-tests/09-tickets.http and 99-rbac-checks.http.
 */
@WebMvcTest(TicketController::class)
@Import(SecurityConfig::class)
class TicketControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var ticketService: TicketService
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder

    // jwt() defaults the `sub` claim to "user".
    private fun fan() = jwt().authorities(SimpleGrantedAuthority("ROLE_fan"))
    private fun staff() = jwt().authorities(SimpleGrantedAuthority("ROLE_staff"))
    private fun platformAdmin() = jwt().authorities(SimpleGrantedAuthority("ROLE_platform-admin"))

    // ----- create (isAuthenticated) -------------------------------------------

    @Test
    fun `create ticket without a token is 401`() {
        mvc.perform(post("/api/v1/tickets").contentType(MediaType.APPLICATION_JSON).content("""{"eventId":1}"""))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `create ticket as an authenticated fan is 201`() {
        doReturn(ticket(ownerSub = "user")).whenever(ticketService).create(any(), any(), any())
        mvc.perform(
            post("/api/v1/tickets").with(fan()).contentType(MediaType.APPLICATION_JSON).content("""{"eventId":1}"""),
        ).andExpect(status().isCreated)
    }

    // ----- pay (owner or staff/platform-admin) --------------------------------

    @Test
    fun `owner can pay their own ticket`() {
        doReturn(ticket(ownerSub = "user")).whenever(ticketService).getById(1L)
        doReturn(ticket(ownerSub = "user")).whenever(ticketService).pay(any(), any(), anyOrNull(), anyOrNull())
        mvc.perform(
            post("/api/v1/tickets/1/pay").with(fan())
                .contentType(MediaType.APPLICATION_JSON).content("""{"paymentMethod":"CARD"}"""),
        ).andExpect(status().isOk)
    }

    @Test
    fun `a fan cannot pay someone else's ticket`() {
        doReturn(ticket(ownerSub = "another-user")).whenever(ticketService).getById(1L)
        mvc.perform(
            post("/api/v1/tickets/1/pay").with(fan())
                .contentType(MediaType.APPLICATION_JSON).content("""{"paymentMethod":"CARD"}"""),
        ).andExpect(status().isForbidden)
    }

    // ----- validate (staff/platform-admin only) -------------------------------

    @Test
    fun `validate is 403 for a fan`() {
        mvc.perform(
            post("/api/v1/tickets/validate").with(fan())
                .contentType(MediaType.APPLICATION_JSON).content("""{"code":"${UUID.randomUUID()}"}"""),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `validate succeeds for staff`() {
        doReturn(ticket(ownerSub = "user")).whenever(ticketService).validate(any(), any())
        mvc.perform(
            post("/api/v1/tickets/validate").with(staff())
                .contentType(MediaType.APPLICATION_JSON).content("""{"code":"${UUID.randomUUID()}"}"""),
        ).andExpect(status().isOk)
    }

    @Test
    fun `validate succeeds for a platform-admin`() {
        doReturn(ticket(ownerSub = "user")).whenever(ticketService).validate(any(), any())
        mvc.perform(
            post("/api/v1/tickets/validate").with(platformAdmin())
                .contentType(MediaType.APPLICATION_JSON).content("""{"code":"${UUID.randomUUID()}"}"""),
        ).andExpect(status().isOk)
    }

    @Test
    fun `validate without a token is 401`() {
        mvc.perform(
            post("/api/v1/tickets/validate").contentType(MediaType.APPLICATION_JSON)
                .content("""{"code":"${UUID.randomUUID()}"}"""),
        ).andExpect(status().isUnauthorized)
    }

    // ----- list by event (staff/platform-admin only) --------------------------

    @Test
    fun `list tickets by event is 403 for a fan`() {
        mvc.perform(get("/api/v1/tickets").param("eventId", "1").with(fan()))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `list tickets by event succeeds for staff`() {
        val page: Page<Ticket> = PageImpl(emptyList())
        doReturn(page).whenever(ticketService).findByEvent(any(), any())
        mvc.perform(get("/api/v1/tickets").param("eventId", "1").with(staff()))
            .andExpect(status().isOk)
    }

    // ----- mine (any authenticated) -------------------------------------------

    @Test
    fun `list mine succeeds for any authenticated user`() {
        val page: Page<Ticket> = PageImpl(emptyList())
        doReturn(page).whenever(ticketService).findByOwner(any(), any())
        mvc.perform(get("/api/v1/tickets/mine").with(fan())).andExpect(status().isOk)
    }

    // ----- getOne (owner or staff/platform-admin) -----------------------------

    @Test
    fun `a fan cannot read someone else's ticket`() {
        doReturn(ticket(ownerSub = "another-user")).whenever(ticketService).getById(1L)
        mvc.perform(get("/api/v1/tickets/1").with(fan())).andExpect(status().isForbidden)
    }

    // -------------------------------------------------------------------------

    private fun ticket(ownerSub: String) = Ticket(
        id = 1L,
        event = Event(id = 1L, matchId = 99L, name = "Demo"),
        price = BigDecimal("10.00"),
        status = TicketStatus.PENDING,
        ownerSub = ownerSub,
    )
}
