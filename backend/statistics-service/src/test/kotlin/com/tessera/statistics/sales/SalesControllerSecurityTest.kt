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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
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
}
