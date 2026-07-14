package com.tessera.ticket.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class MatchLookupClient(
    @Value("\${tessera.match-service.base-url}") private val baseUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val rest = RestTemplate()

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MatchView(
        val id: Long? = null,
        val homeClubId: Long? = null,
        val awayClubId: Long? = null,
        val kickoffAt: String? = null,
        val status: String? = null,
    )

    fun find(matchId: Long): MatchView? =
        try {
            rest.getForObject("$baseUrl/api/v1/matches/$matchId", MatchView::class.java)
        } catch (e: Exception) {
            log.warn("Failed to resolve match {}: {}", matchId, e.message)
            null
        }

    fun homeClubId(matchId: Long): Long? = find(matchId)?.homeClubId

    /**
     * Verifies against the current Keycloak state (not the caller's token) that [userId]
     * is a staff member of [clubId] right now. The caller's bearer token is forwarded so
     * match-service authorises the members lookup, but the returned list is authoritative:
     * a user removed from the club is rejected even if their token still carries the group.
     * Fails closed — any error (unauthorised, unreachable) yields false.
     */
    fun isCurrentStaff(clubId: Long, userId: String, bearerToken: String): Boolean =
        try {
            val headers = HttpHeaders().apply { setBearerAuth(bearerToken) }
            val resp = rest.exchange(
                "$baseUrl/api/v1/clubs/$clubId/members",
                HttpMethod.GET,
                HttpEntity<Void>(headers),
                ClubMembersView::class.java,
            )
            resp.body?.staff.orEmpty().any { it.userId == userId }
        } catch (e: Exception) {
            log.warn("Failed to verify staff membership of {} in club {}: {}", userId, clubId, e.message)
            false
        }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ClubMembersView(val staff: List<ClubMemberView> = emptyList())

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ClubMemberView(val userId: String? = null)
}

fun Jwt.realmRoles(): List<String> =
    getClaimAsStringList("roles")
        ?: (getClaim<Map<String, Any>?>("realm_access")?.get("roles") as? List<*>)
            ?.filterIsInstance<String>()
        ?: emptyList()

fun Jwt.isPlatformAdmin(): Boolean = "platform-admin" in realmRoles()

fun Jwt.managedClubIds(): Set<Long> = clubIdsForGroup("managers")

fun Jwt.staffClubIds(): Set<Long> = clubIdsForGroup("staff")

private fun Jwt.clubIdsForGroup(role: String): Set<Long> =
    (getClaimAsStringList("groups") ?: emptyList()).mapNotNull { path ->
        val parts = path.trim('/').split('/')
        if (parts.size == 3 && parts[0] == "clubs" && parts[2].lowercase() == role) {
            parts[1].toLongOrNull()
        } else {
            null
        }
    }.toSet()
