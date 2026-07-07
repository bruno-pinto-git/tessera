package com.tessera.statistics.sales

import org.springframework.security.oauth2.jwt.Jwt

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
