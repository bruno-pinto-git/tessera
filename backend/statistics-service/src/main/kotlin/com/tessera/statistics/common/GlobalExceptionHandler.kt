package com.tessera.statistics.common

import com.tessera.statistics.matchhistory.MatchHistoryNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

/**
 * Mirrors the RFC 7807 Problem handling used in match-service / bff-service
 * so all backend services produce identical error payloads.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MatchHistoryNotFoundException::class)
    fun handleNotFound(ex: RuntimeException, req: WebRequest): ResponseEntity<Problem> =
        problem(HttpStatus.NOT_FOUND, "https://tessera/api/errors/not-found",
            "Resource not found", ex.message ?: "Resource not found", req)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException, req: WebRequest): ResponseEntity<Problem> =
        problem(HttpStatus.BAD_REQUEST, "https://tessera/api/errors/bad-request",
            "Bad request", "Parameter '${ex.name}' has invalid value '${ex.value}'.", req)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException, req: WebRequest): ResponseEntity<Problem> =
        problem(HttpStatus.BAD_REQUEST, "https://tessera/api/errors/bad-request",
            "Bad request", ex.message ?: "Bad request", req)

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuth(ex: AuthenticationException, req: WebRequest): ResponseEntity<Problem> =
        problem(HttpStatus.UNAUTHORIZED, "https://tessera/api/errors/unauthorized",
            "Unauthorized", ex.message ?: "Authentication required.", req)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleForbidden(ex: AccessDeniedException, req: WebRequest): ResponseEntity<Problem> =
        problem(HttpStatus.FORBIDDEN, "https://tessera/api/errors/forbidden",
            "Forbidden", ex.message ?: "Insufficient permissions.", req)

    @ExceptionHandler(Exception::class)
    fun handleUnknown(ex: Exception, req: WebRequest): ResponseEntity<Problem> =
        problem(HttpStatus.INTERNAL_SERVER_ERROR, "https://tessera/api/errors/internal",
            "Internal server error", ex.message ?: "Unexpected error.", req)

    private fun problem(
        status: HttpStatus,
        type: String,
        title: String,
        detail: String,
        req: WebRequest,
    ): ResponseEntity<Problem> = ResponseEntity
        .status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(Problem(
            type     = type,
            title    = title,
            status   = status.value(),
            detail   = detail,
            instance = req.getDescription(false).removePrefix("uri="),
        ))
}

data class Problem(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String,
)
