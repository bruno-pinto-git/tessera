package com.tessera.ticket.ticket

import com.tessera.ticket.common.PageEnvelope
import com.tessera.ticket.event.isPlatformAdmin
import com.tessera.ticket.event.staffClubIds
import com.tessera.ticket.wallet.GoogleWalletClient
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
    val phoneNumber: String? = null,
)

data class ValidateTicketRequest(
    @field:NotBlank val code: String?,
)

data class WalletPassRequest(
    @field:NotBlank val eventTitle: String?,
    val venue: String? = null,
    val kickoffAt: String? = null,
    val tierLabel: String? = null,
)

data class WalletPassResponse(val saveUrl: String)

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
    val checkoutUrl: String? = null,
)

@RestController
@RequestMapping("/api/v1/tickets")
class TicketController(
    private val ticketService: TicketService,
    private val googleWalletClient: GoogleWalletClient,
) {

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

    @PostMapping("/{id}/pay")
    @PreAuthorize("isAuthenticated()")
    fun pay(
        @PathVariable id: Long,
        @RequestBody request: PayTicketRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): TicketResponse {
        val ticket = ticketService.getById(id)
        if (!isOwnerOrPrivileged(jwt, ticket)) {
            throw AccessDeniedException("Only the ticket owner or staff/platform-admin can pay for this ticket.")
        }
        val method = request.paymentMethod ?: throw IllegalArgumentException("paymentMethod is required")
        val result = ticketService.pay(id, method, request.phoneNumber, request.mbwayReference)
        val updated = result.ticket
        log.info("Pay ticket id={} method={} status={}", updated.id, updated.paymentMethod, updated.status)
        return toResponse(updated, checkoutUrl = result.checkoutUrl)
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAnyRole('staff', 'platform-admin')")
    fun validate(
        @RequestBody request: ValidateTicketRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): TicketResponse {
        val raw = request.code ?: throw IllegalArgumentException("code is required")
        val uuid = try { UUID.fromString(raw) }
            catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid UUID format: $raw")
            }
        val validated =
            ticketService.validate(uuid, userIdOf(jwt), jwt.isPlatformAdmin(), jwt.staffClubIds())
        log.info("Validated ticket id={} by validator={}", validated.id, userIdOf(jwt))
        return toResponse(validated)
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    fun listMine(
        @AuthenticationPrincipal jwt: Jwt,
        pageable: Pageable,
    ): PageEnvelope<TicketResponse> {
        val page = ticketService.findByOwner(userIdOf(jwt), pageable)
        return PageEnvelope.of(page) { toResponse(it) }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('staff', 'platform-admin')")
    fun listByEvent(
        @RequestParam eventId: Long,
        pageable: Pageable,
    ): PageEnvelope<TicketResponse> {
        val page = ticketService.findByEvent(eventId, pageable)
        return PageEnvelope.of(page) { toResponse(it) }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    fun getOne(
        @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt,
    ): TicketResponse {
        val existing = ticketService.getById(id)
        if (!isOwnerOrPrivileged(jwt, existing)) {
            throw AccessDeniedException("You can only access your own tickets.")
        }
        return toResponse(ticketService.getByIdRefreshed(id))
    }

    @PostMapping("/{id}/wallet-pass")
    @PreAuthorize("isAuthenticated()")
    fun walletPass(
        @PathVariable id: Long,
        @RequestBody request: WalletPassRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): WalletPassResponse {
        val ticket = ticketService.getById(id)
        if (!isOwnerOrPrivileged(jwt, ticket)) {
            throw AccessDeniedException("You can only add your own tickets to Google Wallet.")
        }
        if (ticket.status !in setOf(TicketStatus.PAID, TicketStatus.VALIDATED)) {
            throw InvalidTicketStatusException("Cannot add a ${ticket.status} ticket to Google Wallet.")
        }
        val eventTitle = request.eventTitle ?: throw IllegalArgumentException("eventTitle is required")
        val saveUrl = googleWalletClient.createSaveUrl(
            ticketCode = ticket.code.toString(),
            eventId = ticket.event?.id ?: 0,
            eventTitle = eventTitle,
            venue = request.venue,
            kickoffAt = request.kickoffAt,
            tierLabel = request.tierLabel ?: "Bilhete",
        )
        log.info("Wallet pass created for ticket id={}", ticket.id)
        return WalletPassResponse(saveUrl)
    }

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
        return "staff" in roles || "platform-admin" in roles
    }

    private fun toResponse(ticket: Ticket, checkoutUrl: String? = null) = TicketResponse(
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
        checkoutUrl     = checkoutUrl,
    )
}
