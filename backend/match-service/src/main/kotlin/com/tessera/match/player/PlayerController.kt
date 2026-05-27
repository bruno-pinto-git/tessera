package com.tessera.match.player

import com.tessera.match.club.PageEnvelope
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder

@RestController
@RequestMapping("/api/v1")
class PlayerController(
    private val service: PlayerService,
) {

    // ----- Collection (nested under team) -----

    @GetMapping("/teams/{teamId}/players")
    fun listByTeam(
        @PathVariable teamId: Long,
        @PageableDefault(size = 50) pageable: Pageable,
    ): PageEnvelope<PlayerResponse> {
        val page = service.listByTeam(teamId, pageable)
        return PageEnvelope(
            content = page.content.map { it.toResponse() },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    /**
     * Lists all active players across all active teams of the given club.
     * Useful for screens that show a club's full squad without exposing the
     * team structure.
     */
    @GetMapping("/clubs/{clubId}/players")
    fun listByClub(
        @PathVariable clubId: Long,
        @PageableDefault(size = 50) pageable: Pageable,
    ): PageEnvelope<PlayerResponse> {
        val page = service.listByClub(clubId, pageable)
        return PageEnvelope(
            content = page.content.map { it.toResponse() },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    @PostMapping("/teams/{teamId}/players")
    @PreAuthorize("@clubAuthz.canManageTeam(authentication, #teamId)")
    fun create(
        @PathVariable teamId: Long,
        @Valid @RequestBody req: PlayerCreateRequest,
    ): ResponseEntity<PlayerResponse> {
        val player = service.create(teamId, req)
        val location = UriComponentsBuilder.fromPath("/api/v1/players/{id}")
            .buildAndExpand(player.id)
            .toUri()
        return ResponseEntity.created(location).body(player.toResponse())
    }

    // ----- Item (flat path) -----

    @GetMapping("/players/{id}")
    fun get(@PathVariable id: Long): PlayerResponse =
        service.get(id).toResponse()

    @PatchMapping("/players/{id}")
    @PreAuthorize("@clubAuthz.canManagePlayer(authentication, #id)")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: PlayerUpdateRequest,
    ): PlayerResponse =
        service.update(id, req).toResponse()

    @DeleteMapping("/players/{id}")
    @PreAuthorize("@clubAuthz.canManagePlayer(authentication, #id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        service.delete(id)
    }
}
