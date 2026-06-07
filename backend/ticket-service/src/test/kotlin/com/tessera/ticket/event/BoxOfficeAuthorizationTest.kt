package com.tessera.ticket.event

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.jwt.Jwt
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for the box-office authorization rules in [EventController]
 * (plus the JWT claim helpers it relies on). Mirrors docs/http-tests/99-rbac-checks.http
 * for the /events endpoint: a platform admin may open a box office for any
 * match; a club manager only for a match where their club is the home team.
 */
class BoxOfficeAuthorizationTest {

    private val service: EventService = mock()
    private val matchLookup: MatchLookupClient = mock()
    private val controller = EventController(service, matchLookup)

    // ----- JWT claim helpers --------------------------------------------------

    @Test
    fun `realmRoles reads the roles claim`() {
        val jwt = jwt(roles = listOf("club-manager", "staff"), groups = emptyList())
        assertEquals(listOf("club-manager", "staff"), jwt.realmRoles())
    }

    @Test
    fun `isPlatformAdmin reflects the roles claim`() {
        assertTrue(jwt(roles = listOf("platform-admin"), groups = emptyList()).isPlatformAdmin())
        assertTrue(!jwt(roles = listOf("club-manager"), groups = emptyList()).isPlatformAdmin())
    }

    @Test
    fun `managedClubIds parses only the manager groups`() {
        val jwt = jwt(
            roles = listOf("club-manager"),
            groups = listOf("/clubs/5/managers", "/clubs/8/staff", "/garbage", "/clubs/x/managers"),
        )
        assertEquals(setOf(5L), jwt.managedClubIds())
    }

    // ----- EventController.create authorization -------------------------------

    @Test
    fun `platform admin can open a box office for any match`() {
        doReturn(event()).whenever(service).create(any())

        controller.create(request(matchId = null), jwt(listOf("platform-admin"), emptyList()))

        verify(service).create(any())
    }

    @Test
    fun `club manager can open a box office for their home match`() {
        whenever(matchLookup.homeClubId(99L)).thenReturn(5L)
        doReturn(event()).whenever(service).create(any())

        controller.create(
            request(matchId = 99L),
            jwt(listOf("club-manager"), listOf("/clubs/5/managers")),
        )

        verify(service).create(any())
    }

    @Test
    fun `club manager cannot open a box office for another club's match`() {
        whenever(matchLookup.homeClubId(99L)).thenReturn(7L)

        assertFailsWith<AccessDeniedException> {
            controller.create(
                request(matchId = 99L),
                jwt(listOf("club-manager"), listOf("/clubs/5/managers")),
            )
        }
        verify(service, never()).create(any())
    }

    @Test
    fun `a non-admin cannot open a box office without a match`() {
        assertFailsWith<AccessDeniedException> {
            controller.create(
                request(matchId = null),
                jwt(listOf("club-manager"), listOf("/clubs/5/managers")),
            )
        }
        verify(service, never()).create(any())
    }

    @Test
    fun `an unresolvable home club is denied`() {
        whenever(matchLookup.homeClubId(99L)).thenReturn(null)

        assertFailsWith<AccessDeniedException> {
            controller.create(
                request(matchId = 99L),
                jwt(listOf("club-manager"), listOf("/clubs/5/managers")),
            )
        }
        verify(service, never()).create(any())
    }

    // -------------------------------------------------------------------------

    private fun request(matchId: Long?) = CreateEventRequest(
        name = "Box office",
        matchId = matchId,
        priceNormal = BigDecimal("10.00"),
        priceSupporter = BigDecimal("5.00"),
        status = null,
    )

    private fun event() = Event(
        id = 1L,
        matchId = 99L,
        name = "Box office",
        priceNormal = BigDecimal("10.00"),
        priceSupporter = BigDecimal("5.00"),
        status = "PUBLISHED",
    )

    private fun jwt(roles: List<String>, groups: List<String>): Jwt =
        Jwt.withTokenValue("t")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .subject("user")
            .claim("roles", roles)
            .claim("groups", groups)
            .build()
}
