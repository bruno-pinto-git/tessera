package com.tessera.match.sheet

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * RBAC web tests for [MatchSheetController]: sheet edits require canEditSheet
 * (manager/staff of either club), while unlocking is platform-admin only.
 */
@WebMvcTest(MatchSheetController::class)
@Import(SecurityConfig::class)
class MatchSheetControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var service: MatchSheetService
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder
    @MockitoBean(name = "clubAuthz") private lateinit var clubAuthz: ClubAuthorizationService

    private val lineupBody = """{"playerId":1,"role":"STARTER"}"""

    private fun staff() = jwt().authorities(SimpleGrantedAuthority("ROLE_staff"))
    private fun admin() = jwt().authorities(SimpleGrantedAuthority("ROLE_platform-admin"))

    @Test
    fun `add lineup without a token is 401`() {
        mvc.perform(post("/api/v1/matches/1/sheet/lineup").contentType(MediaType.APPLICATION_JSON).content(lineupBody))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `add lineup is 403 when not allowed to edit the sheet`() {
        whenever(clubAuthz.canEditSheet(any(), any())).thenReturn(false)
        mvc.perform(
            post("/api/v1/matches/1/sheet/lineup").with(staff())
                .contentType(MediaType.APPLICATION_JSON).content(lineupBody),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `add lineup succeeds when allowed to edit the sheet`() {
        whenever(clubAuthz.canEditSheet(any(), any())).thenReturn(true)
        doReturn(LineupEntry(id = LineupEntryId(100L, 1L), teamId = 1L, role = LineupRole.STARTER))
            .whenever(service).addLineupEntry(any(), any())
        mvc.perform(
            post("/api/v1/matches/1/sheet/lineup").with(staff())
                .contentType(MediaType.APPLICATION_JSON).content(lineupBody),
        ).andExpect(status().is2xxSuccessful)
    }

    @Test
    fun `unlock is 403 for a non-admin even if they can edit the sheet`() {
        whenever(clubAuthz.canEditSheet(any(), any())).thenReturn(true)
        mvc.perform(post("/api/v1/matches/1/sheet/unlock").with(staff())).andExpect(status().isForbidden)
    }

    @Test
    fun `unlock succeeds for a platform-admin`() {
        doReturn(MatchSheet(id = 100L, matchId = 1L)).whenever(service).unlock(any())
        doReturn(emptyList<LineupEntry>()).whenever(service).listLineup(any())
        doReturn(emptyList<Occurrence>()).whenever(service).listOccurrences(any())
        mvc.perform(post("/api/v1/matches/1/sheet/unlock").with(admin())).andExpect(status().isOk)
    }
}
