package com.tessera.match.club

import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder

data class PageEnvelope<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@RestController
@RequestMapping("/api/v1/clubs")
class ClubController(
    private val service: ClubService,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) name: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PageEnvelope<ClubResponse> {
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
    fun get(@PathVariable id: Long): ClubResponse =
        service.get(id).toResponse()

    @PostMapping
    @PreAuthorize("hasRole('admin')")
    fun create(@Valid @RequestBody req: ClubCreateRequest): ResponseEntity<ClubResponse> {
        val club = service.create(req)
        val location = UriComponentsBuilder.fromPath("/api/v1/clubs/{id}")
            .buildAndExpand(club.id)
            .toUri()
        return ResponseEntity.created(location).body(club.toResponse())
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody req: ClubUpdateRequest,
    ): ClubResponse {
        require(req.name != null || req.foundedYear != null || req.crestUrl != null) {
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
