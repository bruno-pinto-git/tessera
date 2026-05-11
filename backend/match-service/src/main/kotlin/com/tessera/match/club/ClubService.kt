package com.tessera.match.club

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class ClubService(
    private val repo: ClubRepository,
) {

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
        return repo.save(club)
    }

    @Transactional
    fun update(id: Long, req: ClubUpdateRequest): Club {
        val club = repo.findActiveById(id) ?: throw ClubNotFoundException(id)
        // Kotlin nullable params can't distinguish "omitted" from "explicit null"
        // without JsonNullable. PATCH semantics here: null/missing = leave alone.
        // To clear a value, send DELETE or use a future PUT endpoint.
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
