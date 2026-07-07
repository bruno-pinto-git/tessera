package com.tessera.ticket.common

import com.tessera.ticket.ticket.EventNotFoundException
import com.tessera.ticket.ticket.InvalidTicketStatusException
import com.tessera.ticket.ticket.SaleClosedException
import com.tessera.ticket.ticket.TicketNotFoundException
import org.slf4j.LoggerFactory
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

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(value = [
        TicketNotFoundException::class,
        EventNotFoundException::class,
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
        InvalidTicketStatusException::class,
        SaleClosedException::class,
    ])
    fun handleConflict(ex: RuntimeException, req: WebRequest): ResponseEntity<Problem> {
        log.warn("Conflict: {}", ex.message)
        return problem(
            HttpStatus.CONFLICT,
            "https://tessera/api/errors/conflict",
            "Conflict",
            ex.message ?: "Conflict",
            req,
        )
    }

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
