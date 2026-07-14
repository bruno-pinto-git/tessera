package com.tessera.match.match

import com.tessera.match.sheet.MatchSheetService
import com.tessera.match.team.TeamNotFoundException
import com.tessera.match.team.TeamRepository
import com.tessera.match.venue.VenueNotFoundException
import com.tessera.match.venue.VenueRepository
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class MatchService(
    private val repo: MatchRepository,
    private val teamRepo: TeamRepository,
    private val venueRepo: VenueRepository,
    @Lazy private val sheetService: MatchSheetService,
) {

    @Transactional(readOnly = true)
    fun list(
        from: OffsetDateTime?,
        to: OffsetDateTime?,
        status: MatchStatus?,
        clubId: Long?,
        pageable: Pageable,
    ): Page<Match> = repo.findFiltered(from, to, status, clubId, pageable)

    @Transactional(readOnly = true)
    fun get(id: Long): Match =
        repo.findActiveById(id) ?: throw MatchNotFoundException(id)

    @Transactional(readOnly = true)
    fun clubIdsForTeams(teamIds: Collection<Long>): Map<Long, Long> {
        if (teamIds.isEmpty()) return emptyMap()
        return teamRepo.findAllById(teamIds.toSet()).associate { it.id to it.clubId }
    }

    @Transactional
    fun create(req: MatchCreateRequest): Match {
        if (req.kickoffAt.isBefore(OffsetDateTime.now())) {
            throw IllegalArgumentException("Kickoff must be in the future.")
        }
        if (req.homeTeamId == req.awayTeamId) {
            throw IllegalArgumentException("Home and away team must be different.")
        }
        if (teamRepo.findActiveById(req.homeTeamId) == null) {
            throw TeamNotFoundException(req.homeTeamId)
        }
        if (teamRepo.findActiveById(req.awayTeamId) == null) {
            throw TeamNotFoundException(req.awayTeamId)
        }
        req.venueId?.let { venueId ->
            if (venueRepo.findActiveById(venueId) == null) {
                throw VenueNotFoundException(venueId)
            }
        }
        val match = Match(
            homeTeamId  = req.homeTeamId,
            awayTeamId  = req.awayTeamId,
            venueId     = req.venueId,
            kickoffAt   = req.kickoffAt,
            refereeName = req.refereeName,
        )
        return repo.save(match)
    }

    @Transactional
    fun update(id: Long, req: MatchUpdateRequest): Match {
        val match = repo.findActiveById(id) ?: throw MatchNotFoundException(id)

        req.status?.let { newStatus ->
            if (newStatus != match.status && !canTransition(match.status, newStatus)) {
                throw InvalidMatchTransitionException(match.status, newStatus)
            }
            val previous = match.status
            match.status = newStatus
            if (newStatus in TERMINAL_STATUSES && previous !in TERMINAL_STATUSES) {
                sheetService.autoLockIfPresent(match.id)
            }
        }
        req.venueId?.let { venueId ->
            if (venueRepo.findActiveById(venueId) == null) {
                throw VenueNotFoundException(venueId)
            }
            match.venueId = venueId
        }
        req.kickoffAt?.let { match.kickoffAt = it }
        req.homeScore?.let { match.homeScore = it }
        req.awayScore?.let { match.awayScore = it }
        req.refereeName?.let { match.refereeName = it }

        if (match.status == MatchStatus.FINISHED &&
            (match.homeScore == null || match.awayScore == null)) {
            throw IllegalArgumentException(
                "Both homeScore and awayScore must be set when status is FINISHED."
            )
        }
        return match
    }

    @Transactional
    fun delete(id: Long) {
        val match = repo.findActiveById(id) ?: throw MatchNotFoundException(id)
        match.deletedAt = OffsetDateTime.now()
    }

    private fun canTransition(from: MatchStatus, to: MatchStatus): Boolean = when (from) {
        MatchStatus.SCHEDULED -> to in setOf(
            MatchStatus.LIVE, MatchStatus.POSTPONED,
            MatchStatus.ABANDONED, MatchStatus.CANCELLED,
        )
        MatchStatus.LIVE      -> to in setOf(MatchStatus.FINISHED, MatchStatus.ABANDONED)
        MatchStatus.POSTPONED -> to == MatchStatus.SCHEDULED
        MatchStatus.FINISHED, MatchStatus.ABANDONED, MatchStatus.CANCELLED -> false
    }

    companion object {
        private val TERMINAL_STATUSES = setOf(
            MatchStatus.FINISHED, MatchStatus.ABANDONED,
            MatchStatus.POSTPONED, MatchStatus.CANCELLED,
        )
    }
}

class MatchNotFoundException(val matchId: Long)
    : RuntimeException("Match not found: $matchId")

class InvalidMatchTransitionException(val from: MatchStatus, val to: MatchStatus)
    : RuntimeException("Invalid status transition: $from -> $to")
