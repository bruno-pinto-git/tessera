package com.tessera.match.iam

import com.tessera.match.club.ClubNotFoundException
import com.tessera.match.club.ClubRepository
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

/**
 * Manages who is a manager or staff member of a specific club. Both roles
 * are represented by Keycloak group membership under `/clubs/<id>/managers`
 * or `/clubs/<id>/staff`, so changes are persisted to Keycloak only.
 *
 * Authorization is club-scoped (`@clubAuthz.canManageClub`): platform-admins
 * manage any club; a club-manager manages their own club but is limited to
 * the STAFF role (they cannot add/remove other managers, nor grant
 * platform-admin). A manager may also create a brand-new user inline, which
 * is provisioned in Keycloak with a temporary password and the `staff` role.
 */
@RestController
@RequestMapping("/api/v1/clubs/{clubId}/members")
class MembershipController(
    private val kcAdmin: KeycloakAdminClient,
    private val clubRepo: ClubRepository,
) {

    data class MemberResponse(
        val userId: String,
        val username: String?,
        val email: String?,
        val firstName: String?,
        val lastName: String?,
        val role: ClubRole,
    )

    data class MembersResponse(
        val managers: List<MemberResponse>,
        val staff: List<MemberResponse>,
    )

    /**
     * Either link an existing user (`userId`) or create a new one inline
     * (`username`/`password`/...). Exactly one path is taken; `userId` wins.
     */
    data class AddMemberRequest(
        val userId: String? = null,
        val role: ClubRole? = null,
        // Inline new-user creation (used when userId is absent):
        val username: String? = null,
        val email: String? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val password: String? = null,
    )

    @GetMapping
    @PreAuthorize("@clubAuthz.canManageClub(authentication, #clubId)")
    fun list(@PathVariable clubId: Long): MembersResponse {
        ensureClubExists(clubId)
        val managers = membersOf(clubId, ClubRole.MANAGER)
        val staff = membersOf(clubId, ClubRole.STAFF)
        return MembersResponse(managers, staff)
    }

    @PostMapping
    @PreAuthorize("@clubAuthz.canManageClub(authentication, #clubId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun add(@PathVariable clubId: Long, @RequestBody req: AddMemberRequest, authentication: Authentication) {
        ensureClubExists(clubId)
        val role = req.role ?: ClubRole.STAFF
        // Non-admins (club-managers) may only manage staff.
        if (!authentication.isPlatformAdmin() && role != ClubRole.STAFF) {
            throw AccessDeniedException("Club managers can only add staff members.")
        }
        val userId = req.userId?.trim()?.takeIf { it.isNotEmpty() }
            ?: createInlineUser(req, role)
        val groupId = groupIdFor(clubId, role)
        kcAdmin.addUserToGroup(userId, groupId)
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@clubAuthz.canManageClub(authentication, #clubId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(
        @PathVariable clubId: Long,
        @PathVariable userId: String,
        @RequestParam role: ClubRole,
        authentication: Authentication,
    ) {
        ensureClubExists(clubId)
        if (!authentication.isPlatformAdmin() && role != ClubRole.STAFF) {
            throw AccessDeniedException("Club managers can only remove staff members.")
        }
        val groupId = groupIdFor(clubId, role)
        kcAdmin.removeUserFromGroup(userId, groupId)
    }

    // -------------------------------------------------------------------------

    /**
     * Provisions a new Keycloak user (temporary password) and assigns the
     * realm role matching the club role, returning the new user id. Rolls
     * back the user if the role assignment fails.
     */
    private fun createInlineUser(req: AddMemberRequest, role: ClubRole): String {
        val username = req.username?.trim()?.takeIf { it.length >= 3 }
            ?: throw IllegalArgumentException("A userId or a new user (username >= 3 chars) is required.")
        val password = req.password?.takeIf { it.length >= 6 }
            ?: throw IllegalArgumentException("New users require a password with at least 6 characters.")
        val realmRoleName = if (role == ClubRole.MANAGER) "club-manager" else "staff"
        val resolvedRole = kcAdmin.fetchRealmRoleOrNull(realmRoleName)
            ?: throw IllegalStateException("Realm role '$realmRoleName' not found in Keycloak.")
        val userId = kcAdmin.createUser(
            username = username,
            email = req.email?.trim()?.takeIf { it.isNotEmpty() },
            firstName = req.firstName?.trim().orEmpty(),
            lastName = req.lastName?.trim().orEmpty(),
            password = password,
        )
        try {
            kcAdmin.assignRealmRolesByObject(userId, listOf(resolvedRole))
        } catch (e: Exception) {
            runCatching { kcAdmin.deleteUser(userId) }
            throw e
        }
        return userId
    }

    private fun Authentication.isPlatformAdmin(): Boolean =
        authorities.any { it.authority == "ROLE_platform-admin" }

    private fun ensureClubExists(clubId: Long) {
        clubRepo.findActiveById(clubId) ?: throw ClubNotFoundException(clubId)
    }

    private fun groupIdFor(clubId: Long, role: ClubRole): String {
        val name = if (role == ClubRole.MANAGER) "managers" else "staff"
        val group = kcAdmin.findGroupByPath("/clubs/$clubId/$name")
            ?: throw IllegalStateException("Keycloak group /clubs/$clubId/$name missing. Was the club created with Tessera?")
        return group.id ?: error("Group has no id.")
    }

    private fun membersOf(clubId: Long, role: ClubRole): List<MemberResponse> {
        val name = if (role == ClubRole.MANAGER) "managers" else "staff"
        val group = kcAdmin.findGroupByPath("/clubs/$clubId/$name") ?: return emptyList()
        val gid = group.id ?: return emptyList()
        return kcAdmin.listGroupMembers(gid).map {
            MemberResponse(
                userId    = it.id ?: "",
                username  = it.username,
                email     = it.email,
                firstName = it.firstName,
                lastName  = it.lastName,
                role      = role,
            )
        }
    }
}