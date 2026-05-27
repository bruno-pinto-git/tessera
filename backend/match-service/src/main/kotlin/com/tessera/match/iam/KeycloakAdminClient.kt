package com.tessera.match.iam

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.Duration
import java.time.Instant

/**
 * Minimal Keycloak Admin REST client. Authenticates via the configured
 * confidential client's service account (`client_credentials` grant) and
 * caches the token until it nears expiry.
 *
 * Only exposes the operations Tessera actually needs (groups + users +
 * realm role assignment). When the feature surface grows, consider
 * swapping for the official `keycloak-admin-client` library.
 */
@Component
class KeycloakAdminClient(
    private val props: KeycloakAdminProperties,
    builder: RestTemplateBuilder,
) {

    private val log = LoggerFactory.getLogger(KeycloakAdminClient::class.java)
    private val rest: RestTemplate = builder
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(10))
        .build()

    private val tokenLock = Any()
    private var cachedToken: CachedToken? = null

    // -------------------------------------------------------------------------
    // Token management
    // -------------------------------------------------------------------------

    fun token(): String {
        val now = Instant.now()
        synchronized(tokenLock) {
            val current = cachedToken
            if (current != null && now.isBefore(current.refreshAt)) {
                return current.accessToken
            }
            val fresh = fetchToken()
            cachedToken = fresh
            return fresh.accessToken
        }
    }

    private fun fetchToken(): CachedToken {
        val url = "${props.baseUrl}/realms/${props.realm}/protocol/openid-connect/token"
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "client_credentials")
            add("client_id", props.clientId)
            add("client_secret", props.clientSecret)
        }
        log.debug("Requesting Keycloak admin token from $url")
        val response = rest.postForObject(url, HttpEntity(body, headers), TokenResponse::class.java)
            ?: throw IllegalStateException("Empty token response from Keycloak.")

        val expiresIn = response.expiresIn.coerceAtLeast(30)
        // Refresh 30s before actual expiry to avoid races.
        val refreshAt = Instant.now().plusSeconds(expiresIn - 30L)
        return CachedToken(response.accessToken, refreshAt)
    }

    // -------------------------------------------------------------------------
    // Groups
    // -------------------------------------------------------------------------

    /**
     * Returns the group matching `path` (e.g. `/clubs`, `/clubs/1`,
     * `/clubs/1/managers`) or `null` if it does not exist.
     */
    fun findGroupByPath(path: String): GroupRepresentation? {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/group-by-path/${path.trimStart('/')}"
        return try {
            rest.exchange(
                URI.create(url),
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                GroupRepresentation::class.java,
            ).body
        } catch (e: HttpClientErrorException.NotFound) {
            null
        }
    }

    /**
     * Creates a top-level group with the given name. Returns its UUID.
     */
    fun createTopLevelGroup(name: String): String {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/groups"
        val headers = authHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val resp = rest.postForEntity(url, HttpEntity(GroupRepresentation(name = name), headers), Void::class.java)
        return resp.headers.location?.path?.substringAfterLast('/')
            ?: error("No Location header on group create response.")
    }

    /**
     * Creates a child group under `parentId`. Returns the new group UUID.
     */
    fun createChildGroup(parentId: String, name: String): String {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/groups/$parentId/children"
        val headers = authHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val resp = rest.postForEntity(url, HttpEntity(GroupRepresentation(name = name), headers), Void::class.java)
        return resp.headers.location?.path?.substringAfterLast('/')
            ?: error("No Location header on subgroup create response.")
    }

    fun deleteGroup(groupId: String) {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/groups/$groupId"
        try {
            rest.exchange(URI.create(url), HttpMethod.DELETE, HttpEntity<Void>(authHeaders()), Void::class.java)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode != HttpStatus.NOT_FOUND) throw e
        }
    }

    /** Lists members of a group, paged. Returns at most `max` users from `first`. */
    fun listGroupMembers(groupId: String, first: Int = 0, max: Int = 100): List<UserRepresentation> {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/groups/$groupId/members?first=$first&max=$max"
        val resp = rest.exchange(
            URI.create(url),
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders()),
            Array<UserRepresentation>::class.java,
        )
        return resp.body?.toList() ?: emptyList()
    }

    // -------------------------------------------------------------------------
    // Users
    // -------------------------------------------------------------------------

    /**
     * Searches Keycloak users. `search` matches against username, email,
     * first/last name. Empty string returns the first page of all users.
     */
    fun searchUsers(search: String?, first: Int = 0, max: Int = 50): List<UserRepresentation> {
        val q = StringBuilder("?first=$first&max=$max")
        if (!search.isNullOrBlank()) q.append("&search=").append(java.net.URLEncoder.encode(search, "UTF-8"))
        val url = "${props.baseUrl}/admin/realms/${props.realm}/users$q"
        val resp = rest.exchange(
            URI.create(url),
            HttpMethod.GET,
            HttpEntity<Void>(authHeaders()),
            Array<UserRepresentation>::class.java,
        )
        return resp.body?.toList() ?: emptyList()
    }

    fun getUser(userId: String): UserRepresentation? {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/users/$userId"
        return try {
            rest.exchange(URI.create(url), HttpMethod.GET, HttpEntity<Void>(authHeaders()), UserRepresentation::class.java).body
        } catch (e: HttpClientErrorException.NotFound) {
            null
        }
    }

    /**
     * Creates a user with the given attributes. Sets the password right
     * after creation; the user is enabled and the password is non-temporary.
     * Returns the new user id.
     */
    fun createUser(
        username: String,
        email: String?,
        firstName: String?,
        lastName: String?,
        password: String,
    ): String {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/users"
        val headers = authHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val body = UserRepresentation(
            username = username,
            email = email,
            firstName = firstName,
            lastName = lastName,
            enabled = true,
            emailVerified = true,
        )
        val resp = rest.postForEntity(url, HttpEntity(body, headers), Void::class.java)
        val userId = resp.headers.location?.path?.substringAfterLast('/')
            ?: error("No Location header on user create response.")

        // Set credential
        val credUrl = "${props.baseUrl}/admin/realms/${props.realm}/users/$userId/reset-password"
        val cred = CredentialRepresentation(type = "password", value = password, temporary = false)
        rest.exchange(URI.create(credUrl), HttpMethod.PUT, HttpEntity(cred, headers), Void::class.java)
        return userId
    }

    fun deleteUser(userId: String) {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/users/$userId"
        try {
            rest.exchange(URI.create(url), HttpMethod.DELETE, HttpEntity<Void>(authHeaders()), Void::class.java)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode != HttpStatus.NOT_FOUND) throw e
        }
    }

    /** Assigns realm-level roles to a user. Roles must already exist in the realm. */
    fun assignRealmRoles(userId: String, roleNames: List<String>) {
        if (roleNames.isEmpty()) return
        val roles = roleNames.map { fetchRealmRole(it) }
        assignRealmRolesByObject(userId, roles)
    }

    /**
     * Same as [assignRealmRoles] but takes pre-fetched [RoleRepresentation]s.
     * Useful when the caller validated the role exists before creating the
     * user, so we can avoid an extra lookup on the happy path AND fail fast
     * before any user is persisted.
     */
    fun assignRealmRolesByObject(userId: String, roles: List<RoleRepresentation>) {
        if (roles.isEmpty()) return
        val url = "${props.baseUrl}/admin/realms/${props.realm}/users/$userId/role-mappings/realm"
        val headers = authHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        rest.postForEntity(url, HttpEntity(roles, headers), Void::class.java)
    }

    /** Returns the realm role with `name` or `null` if it doesn't exist. */
    fun fetchRealmRoleOrNull(name: String): RoleRepresentation? {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/roles/$name"
        return try {
            rest.exchange(URI.create(url), HttpMethod.GET, HttpEntity<Void>(authHeaders()), RoleRepresentation::class.java).body
        } catch (e: HttpClientErrorException.NotFound) {
            null
        }
    }

    private fun fetchRealmRole(name: String): RoleRepresentation =
        fetchRealmRoleOrNull(name) ?: error("Realm role '$name' not found.")

    fun addUserToGroup(userId: String, groupId: String) {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/users/$userId/groups/$groupId"
        rest.exchange(URI.create(url), HttpMethod.PUT, HttpEntity<Void>(authHeaders()), Void::class.java)
    }

    fun removeUserFromGroup(userId: String, groupId: String) {
        val url = "${props.baseUrl}/admin/realms/${props.realm}/users/$userId/groups/$groupId"
        try {
            rest.exchange(URI.create(url), HttpMethod.DELETE, HttpEntity<Void>(authHeaders()), Void::class.java)
        } catch (e: HttpStatusCodeException) {
            if (e.statusCode != HttpStatus.NOT_FOUND) throw e
        }
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private fun authHeaders() = HttpHeaders().apply { setBearerAuth(token()) }

    private data class CachedToken(val accessToken: String, val refreshAt: Instant)

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class TokenResponse(
        @JsonProperty("access_token") val accessToken: String,
        @JsonProperty("expires_in") val expiresIn: Int,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class GroupRepresentation(
        val id: String? = null,
        val name: String? = null,
        val path: String? = null,
        val subGroups: List<GroupRepresentation>? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class UserRepresentation(
        val id: String? = null,
        val username: String? = null,
        val email: String? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val enabled: Boolean? = null,
        val emailVerified: Boolean? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CredentialRepresentation(
        val type: String,
        val value: String,
        val temporary: Boolean,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class RoleRepresentation(
        val id: String? = null,
        val name: String? = null,
        val composite: Boolean? = null,
        val clientRole: Boolean? = null,
        val containerId: String? = null,
    )
}