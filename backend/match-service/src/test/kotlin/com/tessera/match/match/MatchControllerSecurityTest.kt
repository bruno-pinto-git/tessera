package com.tessera.match.match

import com.tessera.match.config.SecurityConfig
import com.tessera.match.iam.ClubAuthorizationService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

/**
 * RBAC web tests for [MatchController]. Writes are gated by the club-scoped
 * `@clubAuthz` bean (canManageTeam for create, canManageMatch for update/delete).
 * Mirrors docs/http-tests/99-rbac-checks.http for /matches.
 */
@WebMvcTest(MatchController::class)
@Import(SecurityConfig::class)
class MatchControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var service: MatchService
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder

    // The @PreAuthorize SpEL resolves a bean literally named "clubAuthz".
    @MockitoBean(name = "clubAuthz") private lateinit var clubAuthz: ClubAuthorizationService

    private val createBody = """{"homeTeamId":1,"awayTeamId":2,"kickoffAt":"2026-12-15T20:00:00Z"}"""
    private val updateBody = """{"status":"LIVE"}"""

    private fun manager() = jwt().authorities(SimpleGrantedAuthority("ROLE_club-manager"))

    @Test
    fun `POST match without a token is 401`() {
        mvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST match is 403 when not the home club's manager`() {
        whenever(clubAuthz.canManageTeam(any(), any())).thenReturn(false)
        mvc.perform(
            post("/api/v1/matches").with(manager()).contentType(MediaType.APPLICATION_JSON).content(createBody),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST match succeeds for the home club's manager`() {
        whenever(clubAuthz.canManageTeam(any(), any())).thenReturn(true)
        doReturn(match()).whenever(service).create(any())
        doReturn(emptyMap<Long, Long>()).whenever(service).clubIdsForTeams(any())

        mvc.perform(
            post("/api/v1/matches").with(manager()).contentType(MediaType.APPLICATION_JSON).content(createBody),
        ).andExpect(status().is2xxSuccessful)
    }

    @Test
    fun `PATCH match is 403 when not the match's manager`() {
        whenever(clubAuthz.canManageMatch(any(), any())).thenReturn(false)
        mvc.perform(
            patch("/api/v1/matches/1").with(manager()).contentType(MediaType.APPLICATION_JSON).content(updateBody),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `PATCH match succeeds for the match's manager`() {
        whenever(clubAuthz.canManageMatch(any(), any())).thenReturn(true)
        doReturn(match()).whenever(service).update(any(), any())
        doReturn(emptyMap<Long, Long>()).whenever(service).clubIdsForTeams(any())

        mvc.perform(
            patch("/api/v1/matches/1").with(manager()).contentType(MediaType.APPLICATION_JSON).content(updateBody),
        ).andExpect(status().isOk)
    }

    @Test
    fun `DELETE match is 403 when not the match's manager`() {
        whenever(clubAuthz.canManageMatch(any(), any())).thenReturn(false)
        mvc.perform(delete("/api/v1/matches/1").with(manager())).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE match succeeds for the match's manager`() {
        whenever(clubAuthz.canManageMatch(any(), any())).thenReturn(true)
        mvc.perform(delete("/api/v1/matches/1").with(manager())).andExpect(status().isNoContent)
    }

    private fun match() = Match(
        id = 1L,
        homeTeamId = 1L,
        awayTeamId = 2L,
        kickoffAt = OffsetDateTime.parse("2026-12-15T20:00:00Z"),
        status = MatchStatus.SCHEDULED,
    )
}
