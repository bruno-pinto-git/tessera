package com.tessera.statistics.sales

import com.tessera.statistics.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.hamcrest.Matchers.nullValue
import java.math.BigDecimal

/**
 * RBAC web tests for [SalesController] — the sales/revenue reports are
 * platform-admin only (they expose figures not for public consumption).
 */
@WebMvcTest(SalesController::class)
@Import(SecurityConfig::class)
class SalesControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var service: SalesService
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder

    private fun admin() = jwt().authorities(SimpleGrantedAuthority("ROLE_platform-admin"))
    private fun fan() = jwt().authorities(SimpleGrantedAuthority("ROLE_fan"))

    @Test
    fun `sales summary without a token is 401`() {
        mvc.perform(get("/api/v1/stats/sales/summary")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `sales summary as a fan is 403`() {
        mvc.perform(get("/api/v1/stats/sales/summary").with(fan())).andExpect(status().isForbidden)
    }

    @Test
    fun `sales summary as platform-admin is 200`() {
        doReturn(SalesSummaryResponse(0, 0, BigDecimal.ZERO, BigDecimal.ZERO))
            .whenever(service).summary()
        mvc.perform(get("/api/v1/stats/sales/summary").with(admin())).andExpect(status().isOk)
    }

    @Test
    fun `sales by-match as a fan is 403`() {
        mvc.perform(get("/api/v1/stats/sales/by-match/5").with(fan())).andExpect(status().isForbidden)
    }

    @Test
    fun `sales by-match as platform-admin is 200`() {
        doReturn(SalesByMatchResponse(5L, 0, 0, BigDecimal.ZERO)).whenever(service).byMatch(any())
        mvc.perform(get("/api/v1/stats/sales/by-match/5").with(admin())).andExpect(status().isOk)
    }

    // ---- by-club: admin OR the club's own manager ----

    /** A token carrying the realm `roles` claim (consumed by isPlatformAdmin). */
    private fun withRoles(vararg roles: String) =
        jwt().jwt { it.claim("roles", roles.toList()) }

    /** A token whose `groups` claim makes the holder a MANAGER of [clubId]. */
    private fun managerOf(clubId: Long) =
        jwt().jwt { it.claim("groups", listOf("/clubs/$clubId/managers")) }

    /** A token whose `groups` claim makes the holder STAFF of [clubId]. */
    private fun staffOf(clubId: Long) =
        jwt().jwt { it.claim("groups", listOf("/clubs/$clubId/staff")) }

    @Test
    fun `sales by-club without a token is 401`() {
        mvc.perform(get("/api/v1/stats/sales/by-club/5")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `sales by-club as platform-admin is 200`() {
        doReturn(SalesByClubResponse(5L, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO))
            .whenever(service).byClub(any())
        mvc.perform(get("/api/v1/stats/sales/by-club/5").with(withRoles("platform-admin")))
            .andExpect(status().isOk)
    }

    @Test
    fun `sales by-club as the club's own manager is 200`() {
        doReturn(SalesByClubResponse(5L, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO))
            .whenever(service).byClub(any())
        mvc.perform(get("/api/v1/stats/sales/by-club/5").with(managerOf(5L)))
            .andExpect(status().isOk)
    }

    @Test
    fun `sales by-club as a manager of another club is 403`() {
        mvc.perform(get("/api/v1/stats/sales/by-club/5").with(managerOf(8L)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `sales by-club as an authenticated fan with no club is 403`() {
        mvc.perform(get("/api/v1/stats/sales/by-club/5").with(fan()))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `sales by-club as the club's own staff is 200 with counts but no revenue`() {
        doReturn(SalesByClubResponse(5L, 7, 4, BigDecimal("52.00"), BigDecimal("0.571")))
            .whenever(service).byClub(any())
        mvc.perform(get("/api/v1/stats/sales/by-club/5").with(staffOf(5L)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sold").value(7))
            .andExpect(jsonPath("$.validated").value(4))
            .andExpect(jsonPath("$.revenue").value(nullValue()))   // hidden from staff
    }

    @Test
    fun `sales by-club as staff of another club is 403`() {
        mvc.perform(get("/api/v1/stats/sales/by-club/5").with(staffOf(8L)))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `sales by-club keeps revenue for managers`() {
        doReturn(SalesByClubResponse(5L, 7, 4, BigDecimal("52.00"), BigDecimal("0.571")))
            .whenever(service).byClub(any())
        mvc.perform(get("/api/v1/stats/sales/by-club/5").with(managerOf(5L)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.revenue").value(52.00))
    }
}
