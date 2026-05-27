package com.tessera.match.sheet

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/matches/{matchId}/sheet")
class MatchSheetController(
    private val service: MatchSheetService,
) {

    @GetMapping
    fun get(@PathVariable matchId: Long): MatchSheetResponse =
        toResponse(service.getOrCreate(matchId))

    @PostMapping("/lineup")
    @PreAuthorize("@clubAuthz.canEditSheet(authentication, #matchId)")
    @ResponseStatus(HttpStatus.CREATED)
    fun addLineup(
        @PathVariable matchId: Long,
        @Valid @RequestBody req: LineupCreateRequest,
    ): LineupEntryResponse =
        service.addLineupEntry(matchId, req).toResponse()

    @PatchMapping("/lineup/{playerId}")
    @PreAuthorize("@clubAuthz.canEditSheet(authentication, #matchId)")
    fun updateLineup(
        @PathVariable matchId: Long,
        @PathVariable playerId: Long,
        @Valid @RequestBody req: LineupUpdateRequest,
    ): LineupEntryResponse {
        require(req.shirtNumber != null || req.role != null) {
            "At least one field must be provided."
        }
        return service.updateLineupEntry(matchId, playerId, req).toResponse()
    }

    @DeleteMapping("/lineup/{playerId}")
    @PreAuthorize("@clubAuthz.canEditSheet(authentication, #matchId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeLineup(
        @PathVariable matchId: Long,
        @PathVariable playerId: Long,
    ) {
        service.removeLineupEntry(matchId, playerId)
    }

    // ----- Occurrences -----

    @PostMapping("/occurrences")
    @PreAuthorize("@clubAuthz.canEditSheet(authentication, #matchId)")
    @ResponseStatus(HttpStatus.CREATED)
    fun addOccurrence(
        @PathVariable matchId: Long,
        @Valid @RequestBody req: OccurrenceCreateRequest,
    ): OccurrenceResponse =
        service.addOccurrence(matchId, req).toResponse()

    @DeleteMapping("/occurrences/{occId}")
    @PreAuthorize("@clubAuthz.canEditSheet(authentication, #matchId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeOccurrence(
        @PathVariable matchId: Long,
        @PathVariable occId: Long,
    ) {
        service.removeOccurrence(matchId, occId)
    }

    // ----- Lock / Unlock -----

    @PostMapping("/lock")
    @PreAuthorize("@clubAuthz.canEditSheet(authentication, #matchId)")
    fun lock(@PathVariable matchId: Long): MatchSheetResponse =
        toResponse(service.lock(matchId))

    @PostMapping("/unlock")
    @PreAuthorize("hasRole('platform-admin')")
    fun unlock(@PathVariable matchId: Long): MatchSheetResponse =
        toResponse(service.unlock(matchId))

    private fun toResponse(sheet: MatchSheet): MatchSheetResponse =
        MatchSheetResponse(
            matchId     = sheet.matchId,
            locked      = sheet.locked,
            lockedAt    = sheet.lockedAt?.toString(),
            lineup      = service.listLineup(sheet.id).map { it.toResponse() },
            occurrences = service.listOccurrences(sheet.id).map { it.toResponse() },
        )
}
