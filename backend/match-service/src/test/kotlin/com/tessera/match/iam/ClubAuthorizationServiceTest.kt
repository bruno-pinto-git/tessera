package com.tessera.match.iam

import com.tessera.match.match.Match
import com.tessera.match.match.MatchRepository
import com.tessera.match.match.MatchStatus
import com.tessera.match.player.Player
import com.tessera.match.player.PlayerPosition
import com.tessera.match.player.PlayerRepository
import com.tessera.match.team.Team
import com.tessera.match.team.TeamCategory
import com.tessera.match.team.TeamRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit test for the SpEL-callable `clubAuthz` bean. All collaborators
 * (repositories) are mocked — the test focuses on the decision logic:
 *
 *   1. platform-admin -> always allowed
 *   2. club-manager + ClubMembership(MANAGER) -> allowed for management
 *   3. either manager or staff -> allowed for sheet operations
 *   4. anything else -> denied
 */
class ClubAuthorizationServiceTest {

    private val teamRepo: TeamRepository = mock()
    private val playerRepo: PlayerRepository = mock()
    private val matchRepo: MatchRepository = mock()
    private val authz = ClubAuthorizationService(
        teamRepo,
        playerRepo,
        matchRepo,
        ClubMembershipExtractor(),
    )

    // ----- canManageClub ------------------------------------------------------

    @Test
    fun `platform admin can manage any club`() {
        assertTrue(authz.canManageClub(platformAdmin(), 1L))
        assertTrue(authz.canManageClub(platformAdmin(), 999L))
    }

    @Test
    fun `club manager can manage their club`() {
        assertTrue(authz.canManageClub(manager(clubId = 1L), 1L))
    }

    @Test
    fun `club manager cannot manage a different club`() {
        assertFalse(authz.canManageClub(manager(clubId = 1L), 2L))
    }

    @Test
    fun `staff membership alone does not grant club management`() {
        assertFalse(authz.canManageClub(staff(clubId = 1L), 1L))
    }

    @Test
    fun `anonymous-ish authentication is denied`() {
        val anon = UsernamePasswordAuthenticationToken("anon", null, emptyList())
        assertFalse(authz.canManageClub(anon, 1L))
    }

    // ----- canManageTeam ------------------------------------------------------

    @Test
    fun `team not found means access denied for non-admins`() {
        whenever(teamRepo.findActiveById(7L)).thenReturn(null)
        assertFalse(authz.canManageTeam(manager(clubId = 1L), 7L))
    }

    @Test
    fun `team management requires manager membership of the team's club`() {
        whenever(teamRepo.findActiveById(7L)).thenReturn(team(id = 7L, clubId = 1L))
        assertTrue(authz.canManageTeam(manager(clubId = 1L), 7L))
        assertFalse(authz.canManageTeam(manager(clubId = 2L), 7L))
    }

    @Test
    fun `platform admin doesn't need to hit the repo for team management`() {
        // No stubbing of teamRepo — platform-admin should short-circuit before
        // the lookup.
        assertTrue(authz.canManageTeam(platformAdmin(), 7L))
    }

    // ----- canManagePlayer ----------------------------------------------------

    @Test
    fun `player management follows the player team club chain`() {
        whenever(playerRepo.findActiveById(11L))
            .thenReturn(player(id = 11L, teamId = 7L))
        whenever(teamRepo.findActiveById(7L))
            .thenReturn(team(id = 7L, clubId = 1L))

        assertTrue(authz.canManagePlayer(manager(clubId = 1L), 11L))
        assertFalse(authz.canManagePlayer(manager(clubId = 2L), 11L))
    }

    // ----- canEditSheet -------------------------------------------------------

    @Test
    fun `sheet edit allowed for managers of either club involved`() {
        val match = match(id = 99L, homeTeamId = 7L, awayTeamId = 8L)
        whenever(matchRepo.findActiveById(99L)).thenReturn(match)
        whenever(teamRepo.findActiveById(7L)).thenReturn(team(id = 7L, clubId = 1L))
        whenever(teamRepo.findActiveById(8L)).thenReturn(team(id = 8L, clubId = 2L))

        assertTrue(authz.canEditSheet(manager(clubId = 1L), 99L))
        assertTrue(authz.canEditSheet(manager(clubId = 2L), 99L))
        assertFalse(authz.canEditSheet(manager(clubId = 3L), 99L))
    }

    @Test
    fun `sheet edit allowed for staff of either club involved`() {
        val match = match(id = 99L, homeTeamId = 7L, awayTeamId = 8L)
        whenever(matchRepo.findActiveById(99L)).thenReturn(match)
        whenever(teamRepo.findActiveById(7L)).thenReturn(team(id = 7L, clubId = 1L))
        whenever(teamRepo.findActiveById(8L)).thenReturn(team(id = 8L, clubId = 2L))

        assertTrue(authz.canEditSheet(staff(clubId = 2L), 99L))
        assertFalse(authz.canEditSheet(staff(clubId = 999L), 99L))
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private fun platformAdmin(): Authentication = jwtAuth(
        roles = listOf("platform-admin"),
        groups = emptyList(),
    )

    private fun manager(clubId: Long): Authentication = jwtAuth(
        roles = listOf("club-manager"),
        groups = listOf("/clubs/$clubId/managers"),
    )

    private fun staff(clubId: Long): Authentication = jwtAuth(
        roles = listOf("staff"),
        groups = listOf("/clubs/$clubId/staff"),
    )

    private fun jwtAuth(roles: List<String>, groups: List<String>): Authentication {
        val jwt = Jwt.withTokenValue("t")
            .header("alg", "none")
            .issuer("test")
            .subject("u")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("groups", groups)
            .build()
        val authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        return JwtAuthenticationToken(jwt, authorities)
    }

    private fun team(id: Long, clubId: Long) = Team(
        id = id,
        clubId = clubId,
        category = TeamCategory.SENIOR_M,
        createdAt = OffsetDateTime.now(),
    )

    private fun player(id: Long, teamId: Long) = Player(
        id = id,
        teamId = teamId,
        firstName = "Test",
        lastName = "Player",
        position = PlayerPosition.MF,
        createdAt = OffsetDateTime.now(),
    )

    private fun match(id: Long, homeTeamId: Long, awayTeamId: Long) = Match(
        id = id,
        homeTeamId = homeTeamId,
        awayTeamId = awayTeamId,
        kickoffAt = OffsetDateTime.now().plusDays(1),
        status = MatchStatus.SCHEDULED,
    )
}