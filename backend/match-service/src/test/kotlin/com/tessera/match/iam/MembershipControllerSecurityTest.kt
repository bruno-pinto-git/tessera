package com.tessera.match.iam

import com.tessera.match.club.Club
import com.tessera.match.club.ClubRepository
import com.tessera.match.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

@WebMvcTest(MembershipController::class)
@Import(SecurityConfig::class)
class MembershipControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var kcAdmin: KeycloakAdminClient
    @MockitoBean private lateinit var clubRepo: ClubRepository
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder
    @MockitoBean(name = "clubAuthz") private lateinit var clubAuthz: ClubAuthorizationService

    private fun admin() = jwt().authorities(SimpleGrantedAuthority("ROLE_platform-admin"))
    private fun manager() = jwt().authorities(SimpleGrantedAuthority("ROLE_club-manager"))

    private fun clubExists() = whenever(clubRepo.findActiveById(1L)).thenReturn(club())
    private fun groupExists() = whenever(kcAdmin.findGroupByPath(any()))
        .thenReturn(KeycloakAdminClient.GroupRepresentation(id = "grp-1"))

    @Test
    fun `list members without view access is denied`() {
        mvc.perform(get("/api/v1/clubs/1/members")).andExpect(status().isForbidden)
    }

    @Test
    fun `list members is 403 without view access`() {
        whenever(clubAuthz.canViewClub(any(), any())).thenReturn(false)
        mvc.perform(get("/api/v1/clubs/1/members").with(manager())).andExpect(status().isForbidden)
    }

    @Test
    fun `list members succeeds with view access`() {
        whenever(clubAuthz.canViewClub(any(), any())).thenReturn(true)
        clubExists()
        mvc.perform(get("/api/v1/clubs/1/members").with(manager())).andExpect(status().isOk)
    }

    @Test
    fun `add member is 403 without manage access`() {
        whenever(clubAuthz.canManageClub(any(), any())).thenReturn(false)
        mvc.perform(
            post("/api/v1/clubs/1/members").with(manager())
                .contentType(MediaType.APPLICATION_JSON).content("""{"userId":"u1","role":"STAFF"}"""),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `a club manager cannot add another manager`() {
        whenever(clubAuthz.canManageClub(any(), any())).thenReturn(true)
        clubExists()
        mvc.perform(
            post("/api/v1/clubs/1/members").with(manager())
                .contentType(MediaType.APPLICATION_JSON).content("""{"userId":"u1","role":"MANAGER"}"""),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `a club manager can add staff`() {
        whenever(clubAuthz.canManageClub(any(), any())).thenReturn(true)
        clubExists()
        groupExists()
        mvc.perform(
            post("/api/v1/clubs/1/members").with(manager())
                .contentType(MediaType.APPLICATION_JSON).content("""{"userId":"u1","role":"STAFF"}"""),
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `a platform admin can add a manager`() {
        whenever(clubAuthz.canManageClub(any(), any())).thenReturn(true)
        clubExists()
        groupExists()
        mvc.perform(
            post("/api/v1/clubs/1/members").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("""{"userId":"u1","role":"MANAGER"}"""),
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `a club manager cannot remove a manager`() {
        whenever(clubAuthz.canManageClub(any(), any())).thenReturn(true)
        clubExists()
        mvc.perform(delete("/api/v1/clubs/1/members/u1").with(manager()).param("role", "MANAGER"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `a platform admin can remove staff`() {
        whenever(clubAuthz.canManageClub(any(), any())).thenReturn(true)
        clubExists()
        groupExists()
        mvc.perform(delete("/api/v1/clubs/1/members/u1").with(admin()).param("role", "STAFF"))
            .andExpect(status().isNoContent)
    }

    private fun club() = Club(id = 1L, name = "Sporting", createdAt = OffsetDateTime.now())
}
