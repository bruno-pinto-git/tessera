package com.tessera.ticket.ticket

import com.tessera.ticket.common.PageEnvelope
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

private val log = LoggerFactory.getLogger(TicketController::class.java)

data class CreateTicketRequest(
    @field:NotNull val eventId: Long?,
    val supporter: Boolean = false,
)

data class PayTicketRequest(
    @field:NotBlank val paymentMethod: String?,
    val mbwayReference: String? = null,
)

data class ValidateTicketRequest(
    @field:NotBlank val code: String?,
)

data class TicketResponse(
    val id: Long,
    val code: String,
    val eventId: Long,
    val matchId: Long?,
    val ownerSub: String?,
    val price: BigDecimal,
    val status: String,
    val paymentMethod: String?,
    val createdAt: OffsetDateTime,
    val paymentDate: OffsetDateTime?,
    val validationDate: OffsetDateTime?,
    val validatorSub: String?,
)

@RestController
@RequestMapping("/api/v1/tickets")
class TicketController(
    private val ticketService: TicketService,
) {

    /**
     * Create a PENDING ticket for the authenticated user.
     * Open to all authenticated roles (fan/staff/admin).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    fun create(
        @RequestBody request: CreateTicketRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): TicketResponse {
        val eventId = request.eventId
            ?: throw IllegalArgumentException("eventId is required")
        val ticket = ticketService.create(eventId, request.supporter, userIdOf(jwt))
        log.info("Created ticket id={} owner={} status=PENDING", ticket.id, userIdOf(jwt))
        return toResponse(ticket)
    }

    /**
     * Transition PENDING → PAID. The buyer (owner) confirms the payment;
     * staff/admin may also pay on behalf of a fan (over-the-counter cash).
     */
    @PostMapping("/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    fun pay(
        @PathVariable id: Long,
        @RequestBody request: PayTicketRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): TicketResponse {
        val ticket = ticketService.getById(id)
        if (!isOwnerOrPrivileged(jwt, ticket)) {
            throw AccessDeniedException("Only the ticket owner or staff/admin can pay for this ticket.")
        }
        val method = request.paymentMethod ?: throw IllegalArgumentException("paymentMethod is required")
        val paid = ticketService.pay(id, method, request.mbwayReference)
        log.info("Paid ticket id={} method={}", paid.id, paid.paymentMethod)
        return toResponse(paid)
    }

    /**
     * Validate a ticket at the gate. Staff/admin only.
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('staff', 'admin')")
    fun validate(
        @RequestBody request: ValidateTicketRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): TicketResponse {
        val raw = request.code ?: throw IllegalArgumentException("code is required")
        val uuid = try { UUID.fromString(raw) }
            catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid UUID format: $raw")
            }
        val validated = ticketService.validate(uuid, userIdOf(jwt))
        log.info("Validated ticket id={} by validator={}", validated.id, userIdOf(jwt))
        return toResponse(validated)
    }

    /**
     * List tickets owned by the authenticated user.
     */
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    fun listMine(
        @AuthenticationPrincipal jwt: Jwt,
        pageable: Pageable,
    ): PageEnvelope<TicketResponse> {
        val page = ticketService.findByOwner(userIdOf(jwt), pageable)
        return PageEnvelope.of(page) { toResponse(it) }
    }

    /**
     * List tickets for an event. Staff/admin only.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('staff', 'admin')")
    fun listByEvent(
        @RequestParam eventId: Long,
        pageable: Pageable,
    ): PageEnvelope<TicketResponse> {
        val page = ticketService.findByEvent(eventId, pageable)
        return PageEnvelope.of(page) { toResponse(it) }
    }

    /**
     * Single-ticket lookup. The caller must be the owner or staff/admin.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getOne(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt,
    ): TicketResponse {
        val ticket = ticketService.getById(id)
        if (!isOwnerOrPrivileged(jwt, ticket)) {
            throw AccessDeniedException("You can only access your own tickets.")
        }
        return toResponse(ticket)
    }

    /**
     * Resolve a stable per-user identifier from the JWT. We prefer the
     * standard `sub` claim, but our Keycloak realm export does not currently
     * emit it, so we fall back to `preferred_username` (and finally the
     * session id) to keep the service usable across realms with different
     * mapper configurations.
     */
    private fun userIdOf(jwt: Jwt): String =
        jwt.subject
            ?: jwt.getClaimAsString("preferred_username")
            ?: jwt.getClaimAsString("sid")
            ?: throw IllegalStateException("JWT has no usable user identifier")

    private fun isOwnerOrPrivileged(jwt: Jwt, ticket: Ticket): Boolean {
        if (ticket.ownerSub == userIdOf(jwt)) return true
        val roles = jwt.getClaimAsStringList("roles")
            ?: jwt.getClaim<Map<String, Any>?>("realm_access")
                ?.get("roles")
                ?.let { @Suppress("UNCHECKED_CAST") (it as List<String>) }
            ?: emptyList()
        return "staff" in roles || "admin" in roles
    }

    private fun toResponse(ticket: Ticket) = TicketResponse(
        id              = ticket.id,
        code            = ticket.code.toString(),
        eventId         = ticket.event?.id ?: 0,
        matchId         = ticket.event?.matchId,
        ownerSub        = ticket.ownerSub,
        price           = ticket.price,
        status          = ticket.status.name,
        paymentMethod   = ticket.paymentMethod,
        createdAt       = ticket.createdAt,
        paymentDate     = ticket.paymentDate,
        validationDate  = ticket.validationDate,
        validatorSub    = ticket.validatorSub,
    )
}
