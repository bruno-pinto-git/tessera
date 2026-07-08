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
        val roles: List<String>,
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

    data class UpdateUserRequest(
        @field:Email val email: String?,
        @field:Size(min = 1, max = 100) val firstName: String?,
        @field:Size(min = 1, max = 100) val lastName: String?,
        val enabled: Boolean?,
        @field:Pattern(regexp = "club-manager|staff",
            message = "role must be 'club-manager' or 'staff'") val role: String?,
        val forcePasswordReset: Boolean?,
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

        val resolvedRole = kcAdmin.fetchRealmRoleOrNull(role)
            ?: throw IllegalArgumentException("Realm role '$role' not found in Keycloak.")

        val userId = kcAdmin.createUser(username, req.email, firstName, lastName, password)
        try {
            kcAdmin.assignRealmRolesByObject(userId, listOf(resolvedRole))
        } catch (e: Exception) {
            runCatching { kcAdmin.deleteUser(userId) }
            throw e
        }

        val summary = toSummary(kcAdmin.getUser(userId) ?: error("User just created not found: $userId"))
        val location = UriComponentsBuilder.fromPath("/api/v1/users/{id}").buildAndExpand(userId).toUri()
        return ResponseEntity.created(location).body(summary)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('platform-admin')")
    fun update(@PathVariable id: String, @Valid @RequestBody req: UpdateUserRequest): UserSummary {
        val current = kcAdmin.getUser(id) ?: throw UserNotFoundException(id)

        val newRole = req.role?.let {
            kcAdmin.fetchRealmRoleOrNull(it)
                ?: throw IllegalArgumentException("Realm role '$it' not found in Keycloak.")
        }

        val requiredActions = if (req.forcePasswordReset == true) {
            ((current.requiredActions ?: emptyList()) + "UPDATE_PASSWORD").distinct()
        } else {
            current.requiredActions
        }
        val merged = current.copy(
            email          = req.email ?: current.email,
            firstName      = req.firstName ?: current.firstName,
            lastName       = req.lastName ?: current.lastName,
            enabled        = req.enabled ?: current.enabled,
            requiredActions = requiredActions,
        )
        kcAdmin.updateUser(id, merged)

        if (newRole != null) {
            val currentManaged = kcAdmin.getRealmRoleNames(id).filter { it in MANAGEABLE_ROLES }
            val toRemove = currentManaged
                .filter { it != newRole.name }
                .mapNotNull { kcAdmin.fetchRealmRoleOrNull(it) }
            kcAdmin.removeRealmRolesByObject(id, toRemove)
            if (newRole.name !in currentManaged) {
                kcAdmin.assignRealmRolesByObject(id, listOf(newRole))
            }
        }

        return toSummary(kcAdmin.getUser(id) ?: error("User just updated not found: $id"))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('platform-admin')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: String) {
        kcAdmin.deleteUser(id)
    }

    private fun toSummary(u: KeycloakAdminClient.UserRepresentation): UserSummary {
        val id = u.id ?: ""
        val roles = if (id.isNotEmpty()) {
            kcAdmin.getEffectiveRealmRoleNames(id).filter { it in APP_ROLES }
        } else {
            emptyList()
        }
        return UserSummary(
            id        = id,
            username  = u.username,
            email     = u.email,
            firstName = u.firstName,
            lastName  = u.lastName,
            enabled   = u.enabled,
            roles     = roles,
        )
    }

    private companion object {
        val APP_ROLES = setOf("platform-admin", "club-manager", "staff", "fan")
        val MANAGEABLE_ROLES = setOf("club-manager", "staff")
    }
}

class UserNotFoundException(val userId: String)
    : RuntimeException("User not found: $userId")