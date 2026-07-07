package com.tessera.bff.me

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/me")
class MeController {

    private val knownRoles = setOf("platform-admin", "club-manager", "staff", "fan")
    private val knownSubgroups = mapOf("managers" to "MANAGER", "staff" to "STAFF")

    data class MeResponse(
        val sub: String,
        val username: String?,
        val email: String?,
        val firstName: String?,
        val lastName: String?,
        val roles: List<String>,
        val clubMemberships: List<ClubMembership>,
    )

    data class ClubMembership(
        val clubId: Long,
        val role: String,
    )

    @GetMapping
    fun me(@AuthenticationPrincipal jwt: Jwt): MeResponse {
        val realmRoles = jwt.getClaimAsStringList("roles")
            ?: jwt.getClaim<Map<String, Any>?>("realm_access")
                ?.get("roles")
                ?.let { @Suppress("UNCHECKED_CAST") (it as List<String>) }
            ?: emptyList()

        val groups = jwt.getClaimAsStringList("groups") ?: emptyList()

        return MeResponse(
            sub             = jwt.subject ?: "",
            username        = jwt.getClaimAsString("preferred_username"),
            email           = jwt.getClaimAsString("email"),
            firstName       = jwt.getClaimAsString("given_name"),
            lastName        = jwt.getClaimAsString("family_name"),
            roles           = realmRoles.filter { it in knownRoles },
            clubMemberships = groups.mapNotNull(::parseGroupPath),
        )
    }

    private fun parseGroupPath(path: String): ClubMembership? {
        val parts = path.trim('/').split('/')
        if (parts.size != 3 || parts[0] != "clubs") return null
        val clubId = parts[1].toLongOrNull() ?: return null
        val role = knownSubgroups[parts[2].lowercase()] ?: return null
        return ClubMembership(clubId, role)
    }
}