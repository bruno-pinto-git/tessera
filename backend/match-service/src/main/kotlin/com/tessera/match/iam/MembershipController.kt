package com.tessera.match.iam

import com.tessera.match.club.ClubNotFoundException
import com.tessera.match.club.ClubRepository
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Manages who is a manager or staff member of a specific club. Both roles
 * are represented by Keycloak group membership under `/clubs/<id>/managers`
 * or `/clubs/<id>/staff`, so changes are persisted to Keycloak only.
 *
 * All endpoints require `platform-admin`. Club-managers can see their own
 * members via `/me`, but they don't add/remove other members in v1.
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

    data class AddMemberRequest(
        @field:NotBlank val userId: String?,
        val role: ClubRole?,
    )

    @GetMapping
    @PreAuthorize("hasRole('platform-admin')")
    fun list(@PathVariable clubId: Long): MembersResponse {
        ensureClubExists(clubId)
        val managers = membersOf(clubId, ClubRole.MANAGER)
        val staff = membersOf(clubId, ClubRole.STAFF)
        return MembersResponse(managers, staff)
    }

    @PostMapping
    @PreAuthorize("hasRole('platform-admin')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun add(@PathVariable clubId: Long, @RequestBody req: AddMemberRequest) {
        ensureClubExists(clubId)
        val userId = req.userId?.trim()
            ?: throw IllegalArgumentException("userId is required.")
        val role = req.role
            ?: throw IllegalArgumentException("role is required (MANAGER or STAFF).")
        val groupId = groupIdFor(clubId, role)
        kcAdmin.addUserToGroup(userId, groupId)
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('platform-admin')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(
        @PathVariable clubId: Long,
        @PathVariable userId: String,
        @RequestParam role: ClubRole,
    ) {
        ensureClubExists(clubId)
        val groupId = groupIdFor(clubId, role)
        kcAdmin.removeUserFromGroup(userId, groupId)
    }

    // -------------------------------------------------------------------------

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