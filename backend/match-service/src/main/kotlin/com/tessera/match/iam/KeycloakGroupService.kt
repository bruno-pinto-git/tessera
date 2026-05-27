package com.tessera.match.iam

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * High-level operations on the per-club Keycloak group tree:
 *
 *   /clubs
 *   /clubs/<clubId>
 *   /clubs/<clubId>/managers
 *   /clubs/<clubId>/staff
 *
 * Managers and staff of a given club are users placed inside the
 * `managers` / `staff` subgroups. The presence in the `groups` claim of
 * their JWT is what `ClubMembershipExtractor` later turns into scope
 * decisions at the API boundary.
 */
@Service
class KeycloakGroupService(
    private val kcAdmin: KeycloakAdminClient,
) {

    private val log = LoggerFactory.getLogger(KeycloakGroupService::class.java)

    /**
     * Ensures `/clubs/<clubId>/{managers,staff}` exist, creating any missing
     * level. Idempotent — safe to call multiple times for the same club.
     */
    fun ensureClubGroups(clubId: Long) {
        val clubsRootId = ensureTopLevelGroup("clubs")
        val clubGroupId = ensureChildGroup(clubsRootId, clubId.toString(), "/clubs/$clubId")
        ensureChildGroup(clubGroupId, "managers", "/clubs/$clubId/managers")
        ensureChildGroup(clubGroupId, "staff", "/clubs/$clubId/staff")
        log.info("Ensured Keycloak groups for club $clubId.")
    }

    /**
     * Best-effort deletion of `/clubs/<clubId>` and its subgroups. Safe to
     * call even if the groups don't exist — failures are logged, not thrown,
     * so callers can fire-and-forget after a club hard-delete.
     */
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