package com.tessera.match.venue

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
@RequestMapping("/api/v1/venues")
class VenueController(
    private val service: VenueService,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) name: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageEnvelope<VenueResponse> {
        val page = service.list(name, pageable)
        return PageEnvelope(
            content = page.content.map { it.toResponse() },
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): VenueResponse =
        service.get(id).toResponse()

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    fun create(@Valid @RequestBody req: VenueCreateRequest): ResponseEntity<VenueResponse> {
        val venue = service.create(req)
        val location = UriComponentsBuilder.fromPath("/api/v1/venues/{id}")
            .buildAndExpand(venue.id)
            .toUri()
        return ResponseEntity.created(location).body(venue.toResponse())
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: VenueUpdateRequest,
    ): VenueResponse {
        require(req.name != null || req.capacity != null || req.address != null) {
            "At least one field must be provided."
        }
        return service.update(id, req).toResponse()
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        service.delete(id)
    }
}
