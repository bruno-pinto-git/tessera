package com.tessera.match.player

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

@WebMvcTest(PlayerController::class)
@Import(SecurityConfig::class)
class PlayerControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var service: PlayerService
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder
    @MockitoBean(name = "clubAuthz") private lateinit var clubAuthz: ClubAuthorizationService

    private val body = """{"firstName":"Joao","lastName":"Silva","position":"MF"}"""
    private fun manager() = jwt().authorities(SimpleGrantedAuthority("ROLE_club-manager"))

    @Test
    fun `POST player without a token is 401`() {
        mvc.perform(post("/api/v1/teams/7/players").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST player is 403 when not a manager of the team`() {
        whenever(clubAuthz.canManageTeam(any(), any())).thenReturn(false)
        mvc.perform(
            post("/api/v1/teams/7/players").with(manager()).contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST player succeeds for a manager of the team`() {
        whenever(clubAuthz.canManageTeam(any(), any())).thenReturn(true)
        doReturn(player()).whenever(service).create(any(), any())
        mvc.perform(
            post("/api/v1/teams/7/players").with(manager()).contentType(MediaType.APPLICATION_JSON).content(body),
        ).andExpect(status().is2xxSuccessful)
    }

    @Test
    fun `DELETE player is 403 when not a manager of the player's club`() {
        whenever(clubAuthz.canManagePlayer(any(), any())).thenReturn(false)
        mvc.perform(delete("/api/v1/players/11").with(manager())).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE player succeeds for a manager of the player's club`() {
        whenever(clubAuthz.canManagePlayer(any(), any())).thenReturn(true)
        mvc.perform(delete("/api/v1/players/11").with(manager())).andExpect(status().is2xxSuccessful)
    }

    private fun player() = Player(
        id = 11L,
        teamId = 7L,
        firstName = "Joao",
        lastName = "Silva",
        position = PlayerPosition.MF,
        createdAt = OffsetDateTime.now(),
    )
}
