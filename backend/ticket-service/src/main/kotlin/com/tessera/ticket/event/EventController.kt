package com.tessera.ticket.event

import com.tessera.ticket.common.PageEnvelope
import com.tessera.ticket.ticket.EventNotFoundException
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.OffsetDateTime

data class CreateEventRequest(
    @field:NotBlank val name: String?,
    val matchId: Long? = null,
    @field:NotNull @field:DecimalMin("0.00") val priceNormal: BigDecimal?,
    @field:NotNull @field:DecimalMin("0.00") val priceSupporter: BigDecimal?,
    /** Optional. Defaults to PUBLISHED so tickets can be purchased immediately. */
    val status: String? = null,
)

data class EventResponse(
    val id: Long,
    val name: String?,
    val matchId: Long?,
    val priceNormal: BigDecimal,
    val priceSupporter: BigDecimal,
    val status: String,
    val createdAt: OffsetDateTime,
)

@Service
class EventService(
    private val repo: EventRepository,
) {
    @Transactional(readOnly = true)
    fun list(pageable: Pageable) = repo.findAll(pageable)

    @Transactional(readOnly = true)
    fun get(id: Long): Event = repo.findById(id)
        .orElseThrow { EventNotFoundException("Event not found: $id") }

    @Transactional
    fun create(req: CreateEventRequest): Event {
        val status = (req.status ?: "PUBLISHED").uppercase()
        if (status !in ALLOWED_STATUS) {
            throw IllegalArgumentException(
                "Invalid status '$status'; expected one of $ALLOWED_STATUS"
            )
        }
        val entity = Event(
            name           = req.name,
            matchId        = req.matchId,
            priceNormal    = req.priceNormal ?: BigDecimal.ZERO,
            priceSupporter = req.priceSupporter ?: BigDecimal.ZERO,
            status         = status,
        )
        return repo.save(entity)
    }

    companion object {
        val ALLOWED_STATUS = setOf("DRAFT", "PUBLISHED", "SALES_CLOSED", "CANCELLED")
    }
}

@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val service: EventService,
) {

    @GetMapping
    fun list(pageable: Pageable): PageEnvelope<EventResponse> {
        val page = service.list(pageable)
        return PageEnvelope.of(page) { toResponse(it) }
    }

    @GetMapping("/{id}")
    fun getOne(@PathVariable id: Long): EventResponse = toResponse(service.get(id))

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('admin')")
    fun create(@RequestBody request: CreateEventRequest): EventResponse =
        toResponse(service.create(request))

    private fun toResponse(e: Event) = EventResponse(
        id              = e.id,
        name            = e.name,
        matchId         = e.matchId,
        priceNormal     = e.priceNormal,
        priceSupporter  = e.priceSupporter,
        status          = e.status,
        createdAt       = e.createdAt,
    )
}
