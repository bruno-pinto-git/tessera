package com.tessera.ticket.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Minimal read-only client to match-service, used to resolve which club a
 * match belongs to so the ticket-service can authorize club managers opening
 * a box office for their own home matches. `GET /api/v1/matches/{id}` is a
 * public endpoint, so no token is forwarded.
 */
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
        /** ISO-8601 kickoff instant, e.g. "2026-05-01T20:00:00Z". */
        val kickoffAt: String? = null,
        val status: String? = null,
    )

    /** Full match view from match-service, or null if it can't be resolved (e.g. deleted). */
    fun find(matchId: Long): MatchView? =
        try {
            rest.getForObject("$baseUrl/api/v1/matches/$matchId", MatchView::class.java)
        } catch (e: Exception) {
            log.warn("Failed to resolve match {}: {}", matchId, e.message)
            null
        }

    /** Club id of the match's home team, or null if it can't be resolved. */
    fun homeClubId(matchId: Long): Long? = find(matchId)?.homeClubId
}

/** Realm roles carried by the token (the custom `roles` claim, with fallback). */
fun Jwt.realmRoles(): List<String> =
    getClaimAsStringList("roles")
        ?: (getClaim<Map<String, Any>?>("realm_access")?.get("roles") as? List<*>)
            ?.filterIsInstance<String>()
        ?: emptyList()

fun Jwt.isPlatformAdmin(): Boolean = "platform-admin" in realmRoles()

/**
 * Club ids for which the token holder is a MANAGER, parsed from the `groups`
 * claim (`/clubs/<id>/managers`). Mirrors match-service's ClubMembershipExtractor.
 */
fun Jwt.managedClubIds(): Set<Long> = clubIdsForGroup("managers")

/**
 * Club ids for which the token holder is STAFF, parsed from the `groups`
 * claim (`/clubs/<id>/staff`). Used to scope ticket validation at the gate.
 */
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
