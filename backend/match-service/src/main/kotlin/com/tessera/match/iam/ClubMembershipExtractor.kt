package com.tessera.match.iam

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class ClubMembershipExtractor {

    fun extract(jwt: Jwt): Set<ClubMembership> {
        val paths = jwt.getClaimAsStringList("groups") ?: return emptySet()
        return paths.mapNotNullTo(mutableSetOf(), ::parse)
    }

    private fun parse(path: String): ClubMembership? {
        val parts = path.trim('/').split('/')
        if (parts.size != 3 || parts[0] != "clubs") return null
        val clubId = parts[1].toLongOrNull() ?: return null
        val role = when (parts[2].lowercase()) {
            "managers" -> ClubRole.MANAGER
            "staff"    -> ClubRole.STAFF
            else       -> return null
        }
        return ClubMembership(clubId, role)
    }
}