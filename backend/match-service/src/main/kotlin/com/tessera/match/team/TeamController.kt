package com.tessera.match.team

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder

@RestController
@RequestMapping("/api/v1")
class TeamController(
    private val service: TeamService,
) {

    @GetMapping("/clubs/{clubId}/teams")
    fun listByClub(@PathVariable clubId: Long): List<TeamResponse> =
        service.listByClub(clubId).map { it.toResponse() }

    @PostMapping("/clubs/{clubId}/teams")
    @PreAuthorize("@clubAuthz.canManageClub(authentication, #clubId)")
    fun create(
        @PathVariable clubId: Long,
        @Valid @RequestBody req: TeamCreateRequest,
    ): ResponseEntity<TeamResponse> {
        val team = service.create(clubId, req)
        val location = UriComponentsBuilder.fromPath("/api/v1/teams/{id}")
            .buildAndExpand(team.id)
            .toUri()
        return ResponseEntity.created(location).body(team.toResponse())
    }

    @GetMapping("/teams/{id}")
    fun get(@PathVariable id: Long): TeamResponse =
        service.get(id).toResponse()

    @PatchMapping("/teams/{id}")
    @PreAuthorize("@clubAuthz.canManageTeam(authentication, #id)")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: TeamUpdateRequest,
    ): TeamResponse {
        require(req.category != null) { "At least one field must be provided." }
        return service.update(id, req).toResponse()
    }

    @DeleteMapping("/teams/{id}")
    @PreAuthorize("@clubAuthz.canManageTeam(authentication, #id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        service.delete(id)
    }
}
