package com.tessera.statistics.matchhistory

import com.tessera.statistics.common.PageEnvelope
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/stats/match-sheets")
class MatchHistoryController(
    private val service: MatchHistoryService,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) clubId: Long?,
        @RequestParam(required = false) playerId: Long?,
        @RequestParam(required = false) season: String?,
        @RequestParam(required = false) status: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageEnvelope<MatchSummaryResponse> {
        val page = service.list(clubId, playerId, season, status, pageable)
        return PageEnvelope.of(page) { it.toResponse() }
    }

    @GetMapping("/{matchId}")
    fun get(@PathVariable matchId: Long): MatchSheetHistoryResponse =
        service.get(matchId)
}
