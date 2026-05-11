package com.tessera.match.venue

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class VenueService(
    private val repo: VenueRepository,
) {

    @Transactional(readOnly = true)
    fun list(name: String?, pageable: Pageable): Page<Venue> =
        if (name.isNullOrBlank()) repo.findAllActive(pageable)
        else repo.findActiveByNameLike(name.trim(), pageable)

    @Transactional(readOnly = true)
    fun get(id: Long): Venue =
        repo.findActiveById(id) ?: throw VenueNotFoundException(id)

    @Transactional
    fun create(req: VenueCreateRequest): Venue {
        if (repo.existsActiveByNameIgnoreCase(req.name.trim())) {
            throw VenueNameConflictException(req.name)
        }
        val venue = Venue(
            name = req.name.trim(),
            capacity = req.capacity,
            address = req.address,
        )
        return repo.save(venue)
    }

    @Transactional
    fun update(id: Long, req: VenueUpdateRequest): Venue {
        val venue = repo.findActiveById(id) ?: throw VenueNotFoundException(id)
        req.name?.let { newName ->
            val trimmed = newName.trim()
            if (!trimmed.equals(venue.name, ignoreCase = true) &&
                repo.existsActiveByNameIgnoreCaseExcluding(trimmed, id)) {
                throw VenueNameConflictException(trimmed)
            }
            venue.name = trimmed
        }
        req.capacity?.let { venue.capacity = it }
        req.address?.let { venue.address = it }
        return venue
    }

    @Transactional
    fun delete(id: Long) {
        val venue = repo.findActiveById(id) ?: throw VenueNotFoundException(id)
        venue.deletedAt = OffsetDateTime.now()
    }
}

class VenueNotFoundException(val venueId: Long)
    : RuntimeException("Venue not found: $venueId")

class VenueNameConflictException(val name: String)
    : RuntimeException("A venue with name '$name' already exists.")
