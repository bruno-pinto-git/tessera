package com.tessera.match.team

import com.tessera.match.club.ClubNotFoundException
import com.tessera.match.club.ClubRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class TeamService(
    private val repo: TeamRepository,
    private val clubRepo: ClubRepository,
) {

    @Transactional(readOnly = true)
    fun listByClub(clubId: Long): List<Team> {
        ensureClubExists(clubId)
        return repo.findActiveByClub(clubId)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Team =
        repo.findActiveById(id) ?: throw TeamNotFoundException(id)

    @Transactional
    fun create(clubId: Long, req: TeamCreateRequest): Team {
        ensureClubExists(clubId)
        if (repo.existsActiveByClubAndCategory(clubId, req.category)) {
            throw TeamCategoryConflictException(clubId, req.category)
        }
        val team = Team(clubId = clubId, category = req.category)
        return repo.save(team)
    }

    @Transactional
    fun update(id: Long, req: TeamUpdateRequest): Team {
        val team = repo.findActiveById(id) ?: throw TeamNotFoundException(id)
        req.category?.let { newCategory ->
            if (newCategory != team.category &&
                repo.existsActiveByClubAndCategoryExcluding(team.clubId, newCategory, id)) {
                throw TeamCategoryConflictException(team.clubId, newCategory)
            }
            team.category = newCategory
        }
        return team
    }

    @Transactional
    fun delete(id: Long) {
        val team = repo.findActiveById(id) ?: throw TeamNotFoundException(id)
        team.deletedAt = OffsetDateTime.now()
    }

    private fun ensureClubExists(clubId: Long) {
        if (clubRepo.findActiveById(clubId) == null) {
            throw ClubNotFoundException(clubId)
        }
    }
}

class TeamNotFoundException(val teamId: Long)
    : RuntimeException("Team not found: $teamId")

class TeamCategoryConflictException(val clubId: Long, val category: TeamCategory)
    : RuntimeException("Club $clubId already has an active team with category $category.")
