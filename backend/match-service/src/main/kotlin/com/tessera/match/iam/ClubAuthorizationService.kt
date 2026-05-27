package com.tessera.match.iam

import com.tessera.match.match.MatchRepository
import com.tessera.match.player.PlayerRepository
import com.tessera.match.team.TeamRepository
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Scope-aware authorization checks exposed to SpEL `@PreAuthorize`
 * expressions as the `clubAuthz` bean. Used by controllers to gate writes
 * that target a specific club's data:
 *
 *   @PreAuthorize("@clubAuthz.canManageTeam(authentication, #id)")
 *   fun update(@PathVariable id: Long, ...) { ... }
 *
 * Resolution order:
 *   1. `platform-admin` realm role → always allowed
 *   2. `club-manager` role with a matching ClubMembership(MANAGER) → allowed
 *      for management operations (CRUD of teams / players / club itself)
 *   3. For sheet operations: above OR `staff` role with a matching
 *      ClubMembership(STAFF) → allowed
 *
 * Returns `false` (which becomes a 403 in the Problem JSON pipeline) for
 * everything else.
 */
@Component("clubAuthz")
class ClubAuthorizationService(
    private val teamRepo: TeamRepository,
    private val playerRepo: PlayerRepository,
    private val matchRepo: MatchRepository,
    private val extractor: ClubMembershipExtractor,
) {

    fun canManageClub(auth: Authentication, clubId: Long): Boolean {
        if (auth.isPlatformAdmin()) return true
        return memberships(auth).any { it.clubId == clubId && it.role == ClubRole.MANAGER }
    }

    @Transactional(readOnly = true)
    fun canManageTeam(auth: Authentication, teamId: Long): Boolean {
        if (auth.isPlatformAdmin()) return true
        val team = teamRepo.findActiveById(teamId) ?: return false
        return canManageClub(auth, team.clubId)
    }

    @Transactional(readOnly = true)
    fun canManagePlayer(auth: Authentication, playerId: Long): Boolean {
        if (auth.isPlatformAdmin()) return true
        val player = playerRepo.findActiveById(playerId) ?: return false
        return canManageTeam(auth, player.teamId)
    }

    /**
     * True for platform admins, or for a manager/staff of either of the
     * two clubs involved in the match.
     */
    @Transactional(readOnly = true)
    fun canEditSheet(auth: Authentication, matchId: Long): Boolean {
        if (auth.isPlatformAdmin()) return true
        val match = matchRepo.findActiveById(matchId) ?: return false
        val involvedClubIds = setOfNotNull(
            teamRepo.findActiveById(match.homeTeamId)?.clubId,
            teamRepo.findActiveById(match.awayTeamId)?.clubId,
        )
        if (involvedClubIds.isEmpty()) return false
        return memberships(auth).any { it.clubId in involvedClubIds }
    }

    // -------------------------------------------------------------------------

    private fun memberships(auth: Authentication): Set<ClubMembership> {
        val jwt = auth.principal as? Jwt ?: return emptySet()
        return extractor.extract(jwt)
    }

    private fun Authentication.isPlatformAdmin(): Boolean =
        authorities.any { it.authority == "ROLE_platform-admin" }
}