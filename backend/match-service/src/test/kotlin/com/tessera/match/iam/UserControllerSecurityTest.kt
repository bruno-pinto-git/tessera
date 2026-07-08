package com.tessera.match.iam

import com.tessera.match.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerSecurityTest {

    @Autowired private lateinit var mvc: MockMvc

    @MockitoBean private lateinit var kcAdmin: KeycloakAdminClient
    @MockitoBean private lateinit var jwtDecoder: JwtDecoder

    private fun admin() = jwt().authorities(SimpleGrantedAuthority("ROLE_platform-admin"))
    private fun fan() = jwt().authorities(SimpleGrantedAuthority("ROLE_fan"))

    @Test
    fun `search users without a token is 401`() {
        mvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `search users as a fan is 403`() {
        mvc.perform(get("/api/v1/users").with(fan())).andExpect(status().isForbidden)
    }

    @Test
    fun `search users as platform-admin is 200`() {
        doReturn(emptyList<KeycloakAdminClient.UserRepresentation>())
            .whenever(kcAdmin).searchUsers(anyOrNull(), any(), any())
        mvc.perform(get("/api/v1/users").with(admin())).andExpect(status().isOk)
    }

    @Test
    fun `create user as a fan is 403`() {
        mvc.perform(
            post("/api/v1/users").with(fan()).contentType(MediaType.APPLICATION_JSON)
                .content(createBody),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `create user as platform-admin provisions and returns the user`() {
        whenever(kcAdmin.fetchRealmRoleOrNull("staff"))
            .thenReturn(KeycloakAdminClient.RoleRepresentation(name = "staff"))
        doReturn("uid-1").whenever(kcAdmin).createUser(any(), anyOrNull(), any(), any(), any(), any())
        whenever(kcAdmin.getUser("uid-1"))
            .thenReturn(KeycloakAdminClient.UserRepresentation(id = "uid-1", username = "newstaff"))
        doReturn(listOf("staff")).whenever(kcAdmin).getEffectiveRealmRoleNames("uid-1")

        mvc.perform(
            post("/api/v1/users").with(admin()).contentType(MediaType.APPLICATION_JSON)
                .content(createBody),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `delete user as a fan is 403`() {
        mvc.perform(delete("/api/v1/users/uid-1").with(fan())).andExpect(status().isForbidden)
    }

    @Test
    fun `delete user as platform-admin is 204`() {
        mvc.perform(delete("/api/v1/users/uid-1").with(admin())).andExpect(status().isNoContent)
    }

    private val createBody =
        """{"username":"newstaff","email":"s@x.pt","firstName":"New","lastName":"Staff","password":"secret1","role":"staff"}"""
}
