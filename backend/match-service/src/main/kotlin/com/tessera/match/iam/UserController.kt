package com.tessera.match.iam

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder

/**
 * Platform-admin facing endpoints to search and create Keycloak users.
 * Stays a thin wrapper around the Keycloak Admin API — no Tessera-side
 * persistence happens here. The `role` field on create accepts only
 * `club-manager` and `staff` since `platform-admin` and `fan` shouldn't
 * be assigned this way (admins via realm-export, fans via default role).
 */
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val kcAdmin: KeycloakAdminClient,
) {

    data class UserSummary(
        val id: String,
        val username: String?,
        val email: String?,
        val firstName: String?,
        val lastName: String?,
        val enabled: Boolean?,
    )

    data class CreateUserRequest(
        @field:NotBlank @field:Size(min = 3, max = 60) val username: String?,
        @field:Email val email: String?,
        @field:NotBlank @field:Size(min = 1, max = 100) val firstName: String?,
        @field:NotBlank @field:Size(min = 1, max = 100) val lastName: String?,
        @field:NotBlank @field:Size(min = 6, max = 200) val password: String?,
        @field:Pattern(regexp = "club-manager|staff",
            message = "role must be 'club-manager' or 'staff'") val role: String?,
    )

    @GetMapping
    @PreAuthorize("hasRole('platform-admin')")
    fun search(
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") first: Int,
        @RequestParam(defaultValue = "20") max: Int,
    ): List<UserSummary> =
        kcAdmin.searchUsers(search, first, max.coerceAtMost(100)).map(::toSummary)

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('platform-admin')")
    fun get(@PathVariable id: String): UserSummary {
        val u = kcAdmin.getUser(id) ?: throw UserNotFoundException(id)
        return toSummary(u)
    }

    @PostMapping
    @PreAuthorize("hasRole('platform-admin')")
    fun create(@Valid @RequestBody req: CreateUserRequest): ResponseEntity<UserSummary> {
        val username = req.username ?: throw IllegalArgumentException("username required")
        val firstName = req.firstName ?: throw IllegalArgumentException("firstName required")
        val lastName = req.lastName ?: throw IllegalArgumentException("lastName required")
        val password = req.password ?: throw IllegalArgumentException("password required")
        val role = req.role ?: throw IllegalArgumentException("role required")

        // Resolve the realm role FIRST. If we can't (missing role / missing
        // permission), fail before creating a user we'd then have to roll back.
        val resolvedRole = kcAdmin.fetchRealmRoleOrNull(role)
            ?: throw IllegalArgumentException("Realm role '$role' not found in Keycloak.")

        val userId = kcAdmin.createUser(username, req.email, firstName, lastName, password)
        try {
            kcAdmin.assignRealmRolesByObject(userId, listOf(resolvedRole))
        } catch (e: Exception) {
            // Roll back: don't leave orphan users with no role attached.
            runCatching { kcAdmin.deleteUser(userId) }
            throw e
        }

        val summary = toSummary(kcAdmin.getUser(userId) ?: error("User just created not found: $userId"))
        val location = UriComponentsBuilder.fromPath("/api/v1/users/{id}").buildAndExpand(userId).toUri()
        return ResponseEntity.created(location).body(summary)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('platform-admin')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: String) {
        kcAdmin.deleteUser(id)
    }

    private fun toSummary(u: KeycloakAdminClient.UserRepresentation) = UserSummary(
        id        = u.id ?: "",
        username  = u.username,
        email     = u.email,
        firstName = u.firstName,
        lastName  = u.lastName,
        enabled   = u.enabled,
    )
}

class UserNotFoundException(val userId: String)
    : RuntimeException("User not found: $userId")