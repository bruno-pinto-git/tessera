package com.tessera.match.common

import com.tessera.match.club.ClubNameConflictException
import com.tessera.match.club.ClubNotFoundException
import com.tessera.match.club.ClubProvisioningException
import com.tessera.match.iam.UserNotFoundException
import com.tessera.match.match.InvalidMatchTransitionException
import com.tessera.match.match.MatchNotFoundException
import com.tessera.match.player.PlayerNotFoundException
import com.tessera.match.player.PlayerShirtConflictException
import com.tessera.match.sheet.LineupConflictException
import com.tessera.match.sheet.LineupEntryNotFoundException
import com.tessera.match.sheet.LineupRoleLimitException
import com.tessera.match.sheet.OccurrenceNotFoundException
import com.tessera.match.sheet.PlayerSentOffException
import com.tessera.match.sheet.SheetLockedException
import com.tessera.match.sheet.SheetNotEditableException
import com.tessera.match.sheet.TooManySubstitutionsException
import com.tessera.match.team.TeamCategoryConflictException
import com.tessera.match.team.TeamNotFoundException
import com.tessera.match.venue.VenueNameConflictException
import com.tessera.match.venue.VenueNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * Maps domain exceptions to RFC 7807 Problem Details responses, matching the
 * OpenAPI contract (application/problem+json).
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(value = [
        ClubNotFoundException::class,
        VenueNotFoundException::class,
        TeamNotFoundException::class,
        PlayerNotFoundException::class,
        MatchNotFoundException::class,
        LineupEntryNotFoundException::class,
        OccurrenceNotFoundException::class,
        UserNotFoundException::class,
    ])
    fun handleNotFound(ex: RuntimeException, req: WebRequest): ResponseEntity<Problem> =
        problem(
            HttpStatus.NOT_FOUND,
            "https://tessera/api/errors/not-found",
            "Resource not found",
            ex.message ?: "Resource not found",
            req,
        )

    @ExceptionHandler(value = [
        ClubNameConflictException::class,
        VenueNameConflictException::class,
        TeamCategoryConflictException::class,
        PlayerShirtConflictException::class,
        InvalidMatchTransitionException::class,
        LineupConflictException::class,
        LineupRoleLimitException::class,
        TooManySubstitutionsException::class,
        PlayerSentOffException::class,
        SheetLockedException::class,
        SheetNotEditableException::class,
    ])
    fun handleConflict(ex: RuntimeException, req: WebRequest): ResponseEntity<Problem> =
        problem(
            HttpStatus.CONFLICT,
            "https://tessera/api/errors/conflict",
            "Conflict",
            ex.message ?: "Conflict",
            req,
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, req: WebRequest): ResponseEntity<Problem> {
        val errors = ex.bindingResult.fieldErrors.map {
            FieldError(field = it.field, message = it.defaultMessage ?: "invalid")
        }
        return problem(
            HttpStatus.BAD_REQUEST,
            "https://tessera/api/errors/validation",
            "Validation failed",
            "One or more fields are invalid.",
            req,
            errors = errors,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException, req: WebRequest): ResponseEntity<Problem> =
        problem(
            HttpStatus.BAD_REQUEST,
            "https://tessera/api/errors/malformed-json",
            "Malformed request body",
            ex.mostSpecificCause.message ?: "Could not parse JSON body.",
            req,
        )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException, req: WebRequest): ResponseEntity<Problem> =
        problem(
            HttpStatus.BAD_REQUEST,
            "https://tessera/api/errors/bad-request",
            "Bad request",
            "Parameter '${ex.name}' has invalid value '${ex.value}'.",
            req,
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException, req: WebRequest): ResponseEntity<Problem> =
        problem(
            HttpStatus.BAD_REQUEST,
            "https://tessera/api/errors/bad-request",
            "Bad request",
            ex.message ?: "Bad request",
            req,
        )

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(ex: AuthenticationException, req: WebRequest): ResponseEntity<Problem> =
        problem(
            HttpStatus.UNAUTHORIZED,
            "https://tessera/api/errors/unauthorized",
            "Unauthorized",
            ex.message ?: "Authentication required.",
            req,
        )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException, req: WebRequest): ResponseEntity<Problem> =
        problem(
            HttpStatus.FORBIDDEN,
            "https://tessera/api/errors/forbidden",
            "Forbidden",
            ex.message ?: "Insufficient permissions.",
            req,
        )

    @ExceptionHandler(ClubProvisioningException::class)
    fun handleClubProvisioning(ex: ClubProvisioningException, req: WebRequest): ResponseEntity<Problem> =
        problem(
            HttpStatus.SERVICE_UNAVAILABLE,
            "https://tessera/api/errors/provisioning",
            "Club provisioning failed",
            "Could not create the Keycloak groups for this club; please retry. " +
                "If the problem persists, check the match-service logs and Keycloak availability.",
            req,
        )

    /** Catch-all so unexpected exceptions still produce Problem JSON. */
    @ExceptionHandler(Exception::class)
    fun handleUnknown(ex: Exception, req: WebRequest): ResponseEntity<Problem> =
        problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "https://tessera/api/errors/internal",
            "Internal server error",
            ex.message ?: "Unexpected error.",
            req,
        )

    private fun problem(
        status: HttpStatus,
        type: String,
        title: String,
        detail: String,
        req: WebRequest,
        errors: List<FieldError>? = null,
    ): ResponseEntity<Problem> = ResponseEntity
        .status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            Problem(
                type = type,
                title = title,
                status = status.value(),
                detail = detail,
                instance = req.getDescription(false).removePrefix("uri="),
                errors = errors,
            )
        )
}

data class Problem(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String,
    val errors: List<FieldError>? = null,
)

data class FieldError(
    val field: String,
    val message: String,
)
