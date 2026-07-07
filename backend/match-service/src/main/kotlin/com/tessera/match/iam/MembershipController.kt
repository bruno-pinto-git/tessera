package com.tessera.match.iam

import com.tessera.match.club.ClubNotFoundException
import com.tessera.match.club.ClubRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

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

    data class AddMemberRequest(
        val userId: String? = null,
        val role: ClubRole? = null,
        @field:Size(min = 3, max = 60) val username: String? = null,
        @field:Email val email: String? = null,
        @field:Size(max = 100) val firstName: String? = null,
        @field:Size(max = 100) val lastName: String? = null,
        @field:Size(min = 6, max = 200) val password: String? = null,
    )

    @GetMapping
    @PreAuthorize("@clubAuthz.canViewClub(authentication, #clubId)")
    fun list(@PathVariable clubId: Long): MembersResponse {
        ensureClubExists(clubId)
        val managers = membersOf(clubId, ClubRole.MANAGER)
        val staff = membersOf(clubId, ClubRole.STAFF)
        return MembersResponse(managers, staff)
    }

    @PostMapping
    @PreAuthorize("@clubAuthz.canManageClub(authentication, #clubId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun add(@PathVariable clubId: Long, @Valid @RequestBody req: AddMemberRequest, authentication: Authentication) {
        ensureClubExists(clubId)
        val role = req.role ?: ClubRole.STAFF
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