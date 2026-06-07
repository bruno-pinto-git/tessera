package com.tessera.match.venue

import com.tessera.match.config.SecurityConfig
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
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

/** RBAC web tests for [VenueController] (writes are platform-admin only). */
@WebMvcTest(VenueController::class)
@Import(SecurityConfig::class)
class VenueControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var service: VenueService
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder

    private val body = """{"name":"Estadio Jose Alvalade","capacity":50000}"""

    private fun admin() = jwt().authorities(SimpleGrantedAuthority("ROLE_platform-admin"))
    private fun fan() = jwt().authorities(SimpleGrantedAuthority("ROLE_fan"))

    @Test
    fun `GET venues is public`() {
        val page: Page<Venue> = PageImpl(emptyList())
        doReturn(page).whenever(service).list(anyOrNull(), any())
        mvc.perform(get("/api/v1/venues")).andExpect(status().isOk)
    }

    @Test
    fun `POST venue without a token is 401`() {
        mvc.perform(post("/api/v1/venues").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST venue as a fan is 403`() {
        mvc.perform(post("/api/v1/venues").with(fan()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST venue as platform-admin succeeds`() {
        doReturn(venue()).whenever(service).create(any())
        mvc.perform(post("/api/v1/venues").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().is2xxSuccessful)
    }

    @Test
    fun `DELETE venue as a fan is 403`() {
        mvc.perform(delete("/api/v1/venues/1").with(fan())).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE venue as platform-admin succeeds`() {
        mvc.perform(delete("/api/v1/venues/1").with(admin())).andExpect(status().is2xxSuccessful)
    }

    private fun venue() = Venue(id = 1L, name = "Estadio", capacity = 50000, createdAt = OffsetDateTime.now())
}
