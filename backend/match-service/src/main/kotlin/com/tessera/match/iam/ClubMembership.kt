package com.tessera.match.iam

/**
 * Membership of an authenticated user in a club, derived from the `groups`
 * claim in their JWT. Mirrors the Keycloak group structure:
 *
 *   /clubs/<clubId>/managers  -> ClubMembership(clubId, MANAGER)
 *   /clubs/<clubId>/staff     -> ClubMembership(clubId, STAFF)
 */
data class ClubMembership(
    val clubId: Long,
    val role: ClubRole,
)

enum class ClubRole { MANAGER, STAFF }