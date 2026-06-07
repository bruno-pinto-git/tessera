package com.tessera.statistics.matchhistory

import com.tessera.statistics.config.SecurityConfig
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
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Web test for [MatchHistoryController]: match-sheet history is public (fans
 * browse it without authentication).
 */
@WebMvcTest(MatchHistoryController::class)
@Import(SecurityConfig::class)
class MatchHistoryControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var service: MatchHistoryService
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `match-sheet history list is public`() {
        val page: Page<MatchSummary> = PageImpl(emptyList())
        doReturn(page).whenever(service).list(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any())
        mvc.perform(get("/api/v1/stats/match-sheets")).andExpect(status().isOk)
    }
}
