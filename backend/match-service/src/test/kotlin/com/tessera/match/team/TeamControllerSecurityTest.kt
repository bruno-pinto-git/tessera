package com.tessera.match.team

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

/** RBAC web tests for [TeamController] (club-scoped via @clubAuthz). */
@WebMvcTest(TeamController::class)
@Import(SecurityConfig::class)
class TeamControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var service: TeamService
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder
    @MockitoBean(name = "clubAuthz") private lateinit var clubAuthz: ClubAuthorizationService

    private val body = """{"category":"SENIOR_M"}"""
    private fun manager() = jwt().authorities(SimpleGrantedAuthority("ROLE_club-manager"))

    @Test
    fun `POST team without a token is 401`() {
        mvc.perform(post("/api/v1/clubs/1/teams").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST team is 403 when not a manager of the club`() {
        whenever(clubAuthz.canManageClub(any(), any())).thenReturn(false)
        mvc.perform(
            post("/api/v1/clubs/1/teams").with(manager()).contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST team succeeds for a manager of the club`() {
        whenever(clubAuthz.canManageClub(any(), any())).thenReturn(true)
        doReturn(team()).whenever(service).create(any(), any())
        mvc.perform(
            post("/api/v1/clubs/1/teams").with(manager()).contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().is2xxSuccessful)
    }

    @Test
    fun `PATCH team is 403 when not a manager of the team`() {
        whenever(clubAuthz.canManageTeam(any(), any())).thenReturn(false)
        mvc.perform(
            patch("/api/v1/teams/7").with(manager()).contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE team succeeds for a manager of the team`() {
        whenever(clubAuthz.canManageTeam(any(), any())).thenReturn(true)
        mvc.perform(delete("/api/v1/teams/7").with(manager())).andExpect(status().is2xxSuccessful)
    }

    private fun team() = Team(id = 7L, clubId = 1L, category = TeamCategory.SENIOR_M, createdAt = OffsetDateTime.now())
}
