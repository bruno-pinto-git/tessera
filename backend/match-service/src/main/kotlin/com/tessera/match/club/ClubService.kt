package com.tessera.match.club

import com.tessera.match.iam.KeycloakGroupService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ClubService(
    private val repo: ClubRepository,
    private val keycloakGroups: KeycloakGroupService,
) {

    private val log = LoggerFactory.getLogger(ClubService::class.java)

    @Transactional(readOnly = true)
    fun list(name: String?, pageable: Pageable): Page<Club> =
        if (name.isNullOrBlank()) repo.findAllActive(pageable)
        else repo.findActiveByNameLike(name.trim(), pageable)

    @Transactional(readOnly = true)
    fun get(id: Long): Club =
        repo.findActiveById(id) ?: throw ClubNotFoundException(id)

    @Transactional
    fun create(req: ClubCreateRequest): Club {
        if (repo.existsActiveByNameIgnoreCase(req.name.trim())) {
            throw ClubNameConflictException(req.name)
        }
        val club = Club(
            name = req.name.trim(),
            foundedYear = req.foundedYear,
            crestUrl = req.crestUrl,
        )
        val saved = repo.save(club)
        try {
            keycloakGroups.ensureClubGroups(saved.id)
        } catch (e: Exception) {
            log.error("Keycloak group provisioning failed for club ${saved.id}; rolling back.", e)
            throw ClubProvisioningException("Failed to provision Keycloak groups for club ${saved.id}", e)
        }
        return saved
    }

    @Transactional
    fun update(id: Long, req: ClubUpdateRequest): Club {
        val club = repo.findActiveById(id) ?: throw ClubNotFoundException(id)
        req.name?.let { newName ->
            val trimmed = newName.trim()
            if (!trimmed.equals(club.name, ignoreCase = true) &&
                repo.existsActiveByNameIgnoreCaseExcluding(trimmed, id)) {
                throw ClubNameConflictException(trimmed)
            }
            club.name = trimmed
        }
        req.foundedYear?.let { club.foundedYear = it }
        req.crestUrl?.let { club.crestUrl = it }
        return club
    }

    @Transactional
    fun delete(id: Long) {
        val club = repo.findActiveById(id) ?: throw ClubNotFoundException(id)
        club.deletedAt = OffsetDateTime.now()
    }
}

class ClubNotFoundException(val clubId: Long)
    : RuntimeException("Club not found: $clubId")

class ClubNameConflictException(val name: String)
    : RuntimeException("A club with name '$name' already exists.")

class ClubProvisioningException(message: String, cause: Throwable)
    : RuntimeException(message, cause)
