package com.tessera.match.club

import com.tessera.match.config.SecurityConfig
import com.tessera.match.iam.KeycloakGroupService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

/**
 * Web-layer (@WebMvcTest) tests for [ClubController]'s RBAC. The whole point is
 * to exercise the real Spring Security filter chain + method security, so the
 * @PreAuthorize("hasRole('platform-admin')") rules are actually enforced — the
 * JUnit counterpart of docs/http-tests/99-rbac-checks.http for /clubs.
 */
@WebMvcTest(ClubController::class)
@Import(SecurityConfig::class)
class ClubControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var service: ClubService

    // Mocked beans the security slice needs to start: the resource server wires
    // a BearerTokenAuthenticationFilter that requires a JwtDecoder, and the
    // ClubService constructor needs a KeycloakGroupService.
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder
    @MockitoBean private lateinit var keycloakGroups: KeycloakGroupService

    private val createBody = """{"name":"Sporting"}"""
    private val updateBody = """{"name":"Sporting CP"}"""

    private fun admin() = jwt().authorities(SimpleGrantedAuthority("ROLE_platform-admin"))
    private fun fan() = jwt().authorities(SimpleGrantedAuthority("ROLE_fan"))

    @Test
    fun `GET clubs is public`() {
        val page: Page<Club> = PageImpl(emptyList())
        doReturn(page).whenever(service).list(anyOrNull(), any())

        mvc.perform(get("/api/v1/clubs")).andExpect(status().isOk)
    }

    @Test
    fun `POST club without a token is 401`() {
        mvc.perform(post("/api/v1/clubs").contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST club as a fan is 403`() {
        mvc.perform(
            post("/api/v1/clubs").with(fan()).contentType(MediaType.APPLICATION_JSON).content(createBody),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST club as platform-admin is 201`() {
        doReturn(club()).whenever(service).create(any())

        mvc.perform(
            post("/api/v1/clubs").with(admin()).contentType(MediaType.APPLICATION_JSON).content(createBody),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `PATCH club as a fan is 403`() {
        mvc.perform(
            patch("/api/v1/clubs/1").with(fan()).contentType(MediaType.APPLICATION_JSON).content(updateBody),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `PATCH club as platform-admin is 200`() {
        doReturn(club()).whenever(service).update(any(), any())

        mvc.perform(
            patch("/api/v1/clubs/1").with(admin()).contentType(MediaType.APPLICATION_JSON).content(updateBody),
        ).andExpect(status().isOk)
    }

    @Test
    fun `DELETE club as a fan is 403`() {
        mvc.perform(delete("/api/v1/clubs/1").with(fan())).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE club as platform-admin is 204`() {
        mvc.perform(delete("/api/v1/clubs/1").with(admin())).andExpect(status().isNoContent)
    }

    private fun club() = Club(id = 1L, name = "Sporting", createdAt = OffsetDateTime.now())
}
