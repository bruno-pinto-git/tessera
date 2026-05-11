package com.tessera.match.match

import com.tessera.match.club.PageEnvelope
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/v1/matches")
class MatchController(
    private val service: MatchService,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: OffsetDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: OffsetDateTime?,
        @RequestParam(required = false) status: MatchStatus?,
        @RequestParam(required = false) clubId: Long?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageEnvelope<MatchResponse> {
        val page = service.list(from, to, status, clubId, pageable)
        return PageEnvelope(
            content = page.content.map { it.toResponse() },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): MatchResponse =
        service.get(id).toResponse()

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    fun create(@Valid @RequestBody req: MatchCreateRequest): ResponseEntity<MatchResponse> {
        val match = service.create(req)
        val location = UriComponentsBuilder.fromPath("/api/v1/matches/{id}")
            .buildAndExpand(match.id)
            .toUri()
        return ResponseEntity.created(location).body(match.toResponse())
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin','staff')")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: MatchUpdateRequest,
    ): MatchResponse = service.update(id, req).toResponse()

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        service.delete(id)
    }
}
