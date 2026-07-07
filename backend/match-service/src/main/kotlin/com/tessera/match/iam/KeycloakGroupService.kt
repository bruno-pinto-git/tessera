package com.tessera.match.iam

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KeycloakGroupService(
    private val kcAdmin: KeycloakAdminClient,
) {

    private val log = LoggerFactory.getLogger(KeycloakGroupService::class.java)

    fun ensureClubGroups(clubId: Long) {
        val clubsRootId = ensureTopLevelGroup("clubs")
        val clubGroupId = ensureChildGroup(clubsRootId, clubId.toString(), "/clubs/$clubId")
        ensureChildGroup(clubGroupId, "managers", "/clubs/$clubId/managers")
        ensureChildGroup(clubGroupId, "staff", "/clubs/$clubId/staff")
        log.info("Ensured Keycloak groups for club $clubId.")
    }

    fun deleteClubGroups(clubId: Long) {
        val path = "/clubs/$clubId"
        try {
            val group = kcAdmin.findGroupByPath(path) ?: return
            kcAdmin.deleteGroup(group.id!!)
            log.info("Deleted Keycloak group $path.")
        } catch (e: Exception) {
            log.warn("Failed to delete Keycloak group $path: ${e.message}")
        }
    }

    private fun ensureTopLevelGroup(name: String): String {
        val existing = kcAdmin.findGroupByPath("/$name")
        if (existing?.id != null) return existing.id
        return kcAdmin.createTopLevelGroup(name)
    }

    private fun ensureChildGroup(parentId: String, name: String, pathForLookup: String): String {
        val existing = kcAdmin.findGroupByPath(pathForLookup)
        if (existing?.id != null) return existing.id
        return kcAdmin.createChildGroup(parentId, name)
    }
}