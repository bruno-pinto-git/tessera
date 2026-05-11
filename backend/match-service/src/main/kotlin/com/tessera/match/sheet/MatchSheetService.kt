package com.tessera.match.sheet

import com.tessera.match.match.Match
import com.tessera.match.match.MatchNotFoundException
import com.tessera.match.match.MatchRepository
import com.tessera.match.match.MatchStatus
import com.tessera.match.player.PlayerNotFoundException
import com.tessera.match.player.PlayerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class MatchSheetService(
    private val sheetRepo: MatchSheetRepository,
    private val lineupRepo: LineupEntryRepository,
    private val occurrenceRepo: OccurrenceRepository,
    private val matchRepo: MatchRepository,
    private val playerRepo: PlayerRepository,
) {

    /**
     * Lazy fetch-or-create. Returns the sheet for the given match, creating
     * an empty one if it does not yet exist.
     */
    @Transactional
    fun getOrCreate(matchId: Long): MatchSheet {
        val match = matchRepo.findActiveById(matchId) ?: throw MatchNotFoundException(matchId)
        return sheetRepo.findByMatchId(matchId)
            ?: sheetRepo.save(MatchSheet(matchId = match.id))
    }

    @Transactional(readOnly = true)
    fun listLineup(sheetId: Long): List<LineupEntry> = lineupRepo.findBySheet(sheetId)

    @Transactional
    fun addLineupEntry(matchId: Long, req: LineupCreateRequest): LineupEntry {
        val (sheet, match) = lockedAwareSheet(matchId)
        val player = playerRepo.findActiveById(req.playerId)
            ?: throw PlayerNotFoundException(req.playerId)

        // Player's team must be one of the match's teams
        if (player.teamId != match.homeTeamId && player.teamId != match.awayTeamId) {
            throw IllegalArgumentException(
                "Player ${player.id} (team ${player.teamId}) does not belong to match $matchId."
            )
        }
        // Already in lineup?
        val existingId = LineupEntryId(matchSheetId = sheet.id, playerId = player.id)
        if (lineupRepo.findById(existingId).isPresent) {
            throw LineupConflictException("Player ${player.id} is already in the lineup.")
        }
        // Shirt number unique per team within the sheet
        val shirt = req.shirtNumber ?: player.shirtNumber
        if (shirt != null && lineupRepo.existsBySheetTeamShirt(sheet.id, player.teamId, shirt)) {
            throw LineupConflictException(
                "Shirt $shirt already assigned in this match for team ${player.teamId}."
            )
        }
        val entry = LineupEntry(
            id           = existingId,
            teamId       = player.teamId,
            shirtNumber  = shirt,
            role         = req.role,
        )
        return lineupRepo.save(entry)
    }

    @Transactional
    fun updateLineupEntry(matchId: Long, playerId: Long, req: LineupUpdateRequest): LineupEntry {
        val (sheet, _) = lockedAwareSheet(matchId)
        val entry = lineupRepo.findById(LineupEntryId(sheet.id, playerId)).orElseThrow {
            LineupEntryNotFoundException(matchId, playerId)
        }
        req.shirtNumber?.let { newShirt ->
            if (newShirt != entry.shirtNumber &&
                lineupRepo.existsBySheetTeamShirtExcluding(sheet.id, entry.teamId, newShirt, playerId)) {
                throw LineupConflictException(
                    "Shirt $newShirt already assigned in this match for team ${entry.teamId}."
                )
            }
            entry.shirtNumber = newShirt
        }
        req.role?.let { entry.role = it }
        return entry
    }

    @Transactional
    fun removeLineupEntry(matchId: Long, playerId: Long) {
        val (sheet, _) = lockedAwareSheet(matchId)
        val key = LineupEntryId(sheet.id, playerId)
        if (!lineupRepo.findById(key).isPresent) {
            throw LineupEntryNotFoundException(matchId, playerId)
        }
        lineupRepo.deleteById(key)
    }

    // -------------------------------------------------------------------
    //  Occurrences
    // -------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun listOccurrences(sheetId: Long): List<Occurrence> = occurrenceRepo.findBySheet(sheetId)

    @Transactional
    fun addOccurrence(matchId: Long, req: OccurrenceCreateRequest): Occurrence {
        val (sheet, _) = lockedAwareSheet(matchId)

        // The author player must be in the lineup of this sheet
        val authorEntry = lineupRepo.findById(LineupEntryId(sheet.id, req.playerId))
            .orElseThrow {
                IllegalArgumentException(
                    "Player ${req.playerId} is not in the lineup of this match."
                )
            }

        // SUBSTITUTION rules
        when (req.type) {
            OccurrenceType.SUBSTITUTION -> {
                val replacedId = req.replacedPlayerId
                    ?: throw IllegalArgumentException(
                        "SUBSTITUTION requires `replacedPlayerId`."
                    )
                if (replacedId == req.playerId) {
                    throw IllegalArgumentException(
                        "`playerId` and `replacedPlayerId` must be different."
                    )
                }
                val replacedEntry = lineupRepo.findById(LineupEntryId(sheet.id, replacedId))
                    .orElseThrow {
                        IllegalArgumentException(
                            "Replaced player $replacedId is not in the lineup of this match."
                        )
                    }
                if (replacedEntry.teamId != authorEntry.teamId) {
                    throw IllegalArgumentException(
                        "Both players in a substitution must belong to the same team."
                    )
                }
            }
            else -> {
                if (req.replacedPlayerId != null) {
                    throw IllegalArgumentException(
                        "`replacedPlayerId` is only allowed for SUBSTITUTION occurrences."
                    )
                }
            }
        }

        val occ = Occurrence(
            matchSheetId      = sheet.id,
            minute            = req.minute,
            type              = req.type,
            teamId            = authorEntry.teamId,
            playerId          = req.playerId,
            replacedPlayerId  = req.replacedPlayerId,
        )
        return occurrenceRepo.save(occ)
    }

    @Transactional
    fun removeOccurrence(matchId: Long, occId: Long) {
        val (sheet, _) = lockedAwareSheet(matchId)
        val occ = occurrenceRepo.findById(occId).orElseThrow {
            OccurrenceNotFoundException(matchId, occId)
        }
        if (occ.matchSheetId != sheet.id) {
            // Defensive: occ exists but belongs to a different sheet
            throw OccurrenceNotFoundException(matchId, occId)
        }
        occurrenceRepo.delete(occ)
    }

    // -------------------------------------------------------------------
    //  Lock / Unlock
    // -------------------------------------------------------------------

    /** Manual lock by staff/admin. Idempotent. */
    @Transactional
    fun lock(matchId: Long): MatchSheet {
        val match = matchRepo.findActiveById(matchId) ?: throw MatchNotFoundException(matchId)
        val sheet = sheetRepo.findByMatchId(matchId)
            ?: sheetRepo.save(MatchSheet(matchId = match.id))
        if (!sheet.locked) {
            sheet.locked = true
            sheet.lockedAt = OffsetDateTime.now()
        }
        return sheet
    }

    /** Manual unlock by admin. Idempotent. */
    @Transactional
    fun unlock(matchId: Long): MatchSheet {
        val match = matchRepo.findActiveById(matchId) ?: throw MatchNotFoundException(matchId)
        val sheet = sheetRepo.findByMatchId(matchId)
            ?: sheetRepo.save(MatchSheet(matchId = match.id))
        if (sheet.locked) {
            sheet.locked = false
            sheet.lockedAt = null
        }
        return sheet
    }

    /**
     * Auto-lock hook called by MatchService when a match transitions into a
     * terminal status. Silent no-op if the sheet does not exist yet.
     */
    @Transactional
    fun autoLockIfPresent(matchId: Long) {
        val sheet = sheetRepo.findByMatchId(matchId) ?: return
        if (!sheet.locked) {
            sheet.locked = true
            sheet.lockedAt = OffsetDateTime.now()
        }
    }

    /**
     * Returns (sheet, match) and validates that the sheet is editable:
     *   - sheet not locked
     *   - match status ∈ {SCHEDULED, LIVE}
     */
    private fun lockedAwareSheet(matchId: Long): Pair<MatchSheet, Match> {
        val match = matchRepo.findActiveById(matchId) ?: throw MatchNotFoundException(matchId)
        val sheet = sheetRepo.findByMatchId(matchId)
            ?: sheetRepo.save(MatchSheet(matchId = match.id))
        if (sheet.locked) throw SheetLockedException(matchId)
        if (match.status !in EDITABLE_STATUSES) {
            throw SheetNotEditableException(matchId, match.status)
        }
        return sheet to match
    }

    companion object {
        private val EDITABLE_STATUSES = setOf(MatchStatus.SCHEDULED, MatchStatus.LIVE)
    }
}

class LineupConflictException(message: String) : RuntimeException(message)

class LineupEntryNotFoundException(matchId: Long, playerId: Long)
    : RuntimeException("Lineup entry not found in match $matchId for player $playerId.")

class SheetLockedException(matchId: Long)
    : RuntimeException("Match sheet for match $matchId is locked.")

class SheetNotEditableException(matchId: Long, status: MatchStatus)
    : RuntimeException("Match sheet for match $matchId cannot be edited (match status: $status).")

class OccurrenceNotFoundException(matchId: Long, occId: Long)
    : RuntimeException("Occurrence $occId not found in match $matchId.")
