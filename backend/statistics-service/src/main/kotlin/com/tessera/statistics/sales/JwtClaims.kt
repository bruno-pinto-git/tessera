package com.tessera.statistics.sales

import org.springframework.security.oauth2.jwt.Jwt

/**
 * JWT claim helpers for club-scoped authorization on sales reports. Mirrors the
 * extraction used by match-service (ClubMembershipExtractor) and ticket-service
 * so a club manager can read their own club's sales without platform-admin.
 */

/** Realm roles carried by the token (the custom `roles` claim, with fallback). */
fun Jwt.realmRoles(): List<String> =
    getClaimAsStringList("roles")
        ?: (getClaim<Map<String, Any>?>("realm_access")?.get("roles") as? List<*>)
            ?.filterIsInstance<String>()
        ?: emptyList()

fun Jwt.isPlatformAdmin(): Boolean = "platform-admin" in realmRoles()

/**
 * Club ids for which the token holder is a MANAGER, parsed from the `groups`
 * claim (`/clubs/<id>/managers`).
 */
fun Jwt.managedClubIds(): Set<Long> = clubIdsForGroup("managers")

/**
 * Club ids for which the token holder is STAFF, parsed from the `groups`
 * claim (`/clubs/<id>/staff`). Staff may read their club's sales counts but
 * not the revenue.
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
