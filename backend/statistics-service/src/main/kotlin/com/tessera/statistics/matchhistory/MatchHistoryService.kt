package com.tessera.statistics.matchhistory

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MatchHistoryService(
    private val summaryRepo: MatchSummaryRepository,
    private val lineupRepo: LineupSnapshotRepository,
    private val occurrenceRepo: OccurrenceSnapshotRepository,
) {

    @Transactional(readOnly = true)
    fun list(
        clubId: Long?,
        playerId: Long?,
        season: String?,
        status: String?,
        pageable: Pageable,
    ): Page<MatchSummary> = summaryRepo.findFiltered(clubId, playerId, season, status, pageable)

    @Transactional(readOnly = true)
    fun get(matchId: Long): MatchSheetHistoryResponse {
        val summary = summaryRepo.findById(matchId).orElseThrow {
            MatchHistoryNotFoundException(matchId)
        }
        return MatchSheetHistoryResponse(
            matchId      = matchId,
            summary      = summary.toResponse(),
            lineup       = lineupRepo.findByMatchId(matchId).map { it.toResponse() },
            occurrences  = occurrenceRepo.findByMatchId(matchId).map { it.toResponse() },
        )
    }
}

class MatchHistoryNotFoundException(matchId: Long)
    : RuntimeException("No match-sheet history for match $matchId.")
