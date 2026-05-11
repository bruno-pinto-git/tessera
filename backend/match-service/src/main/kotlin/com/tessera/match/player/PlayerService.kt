package com.tessera.match.player

import com.tessera.match.club.ClubNotFoundException
import com.tessera.match.club.ClubRepository
import com.tessera.match.team.TeamNotFoundException
import com.tessera.match.team.TeamRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class PlayerService(
    private val repo: PlayerRepository,
    private val teamRepo: TeamRepository,
    private val clubRepo: ClubRepository,
) {

    @Transactional(readOnly = true)
    fun listByTeam(teamId: Long, pageable: Pageable): Page<Player> {
        ensureTeamExists(teamId)
        return repo.findActiveByTeam(teamId, pageable)
    }

    @Transactional(readOnly = true)
    fun listByClub(clubId: Long, pageable: Pageable): Page<Player> {
        if (clubRepo.findActiveById(clubId) == null) {
            throw ClubNotFoundException(clubId)
        }
        return repo.findActiveByClub(clubId, pageable)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): Player =
        repo.findActiveById(id) ?: throw PlayerNotFoundException(id)

    @Transactional
    fun create(teamId: Long, req: PlayerCreateRequest): Player {
        ensureTeamExists(teamId)
        req.shirtNumber?.let { number ->
            if (repo.existsActiveByTeamAndShirt(teamId, number)) {
                throw PlayerShirtConflictException(teamId, number)
            }
        }
        val player = Player(
            teamId = teamId,
            firstName = req.firstName.trim(),
            lastName = req.lastName.trim(),
            birthdate = req.birthdate,
            nationality = req.nationality,
            position = req.position,
            shirtNumber = req.shirtNumber,
            photoUrl = req.photoUrl,
            dominantFoot = req.dominantFoot,
            height = req.height,
            weight = req.weight,
            status = req.status ?: PlayerStatus.ACTIVE,
        )
        return repo.save(player)
    }

    @Transactional
    fun update(id: Long, req: PlayerUpdateRequest): Player {
        val player = repo.findActiveById(id) ?: throw PlayerNotFoundException(id)

        req.shirtNumber?.let { number ->
            if (number != player.shirtNumber &&
                repo.existsActiveByTeamAndShirtExcluding(player.teamId, number, id)) {
                throw PlayerShirtConflictException(player.teamId, number)
            }
            player.shirtNumber = number
        }
        req.firstName?.let { player.firstName = it.trim() }
        req.lastName?.let { player.lastName = it.trim() }
        req.birthdate?.let { player.birthdate = it }
        req.nationality?.let { player.nationality = it }
        req.position?.let { player.position = it }
        req.photoUrl?.let { player.photoUrl = it }
        req.dominantFoot?.let { player.dominantFoot = it }
        req.height?.let { player.height = it }
        req.weight?.let { player.weight = it }
        req.status?.let { player.status = it }
        return player
    }

    @Transactional
    fun delete(id: Long) {
        val player = repo.findActiveById(id) ?: throw PlayerNotFoundException(id)
        player.deletedAt = OffsetDateTime.now()
    }

    private fun ensureTeamExists(teamId: Long) {
        if (teamRepo.findActiveById(teamId) == null) {
            throw TeamNotFoundException(teamId)
        }
    }
}

class PlayerNotFoundException(val playerId: Long)
    : RuntimeException("Player not found: $playerId")

class PlayerShirtConflictException(val teamId: Long, val shirtNumber: Int)
    : RuntimeException("Shirt number $shirtNumber is already in use in team $teamId.")
