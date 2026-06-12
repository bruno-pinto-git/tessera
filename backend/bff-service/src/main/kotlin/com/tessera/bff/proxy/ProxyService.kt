package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Forwards an incoming request to a downstream service while preserving:
 *   - HTTP method
 *   - request body (if any)
 *   - query string
 *   - the Authorization header (so JWT pass-through works)
 *
 * The downstream URL is built from `baseUrl` + the original request URI.
 * Both 4xx and 5xx responses from downstream are surfaced verbatim so the
 * caller sees the same status and Problem JSON body the underlying service
 * produced.
 */
@Service
class ProxyService(
    private val restTemplate: RestTemplate,
) {

    fun forward(
        request: HttpServletRequest,
        baseUrl: String,
        body: String?,
    ): ResponseEntity<String> {
        val targetUrl = buildString {
            append(baseUrl)
            append(request.requestURI)
            request.queryString?.let { append('?').append(it) }
        }
        // Wrap in a URI so RestTemplate treats it as already-encoded and does
        // NOT re-encode it. Passing the raw String would double-encode any
        // percent-escapes already present in the query (e.g. the `%3A` colons
        // in ISO timestamps on /stats/sales/range), producing `%253A` and a
        // 400 downstream.
        val targetUri = URI.create(targetUrl)

        val headers = HttpHeaders().apply {
            // Forward Authorization (JWT) and Content-Type only. We
            // deliberately drop other headers (Host, Connection, ...).
            request.getHeader(HttpHeaders.AUTHORIZATION)?.let {
                set(HttpHeaders.AUTHORIZATION, it)
            }
            request.getHeader(HttpHeaders.CONTENT_TYPE)?.let {
                set(HttpHeaders.CONTENT_TYPE, it)
            } ?: run {
                // Default to JSON when sending a body but no Content-Type set
                if (!body.isNullOrEmpty()) contentType = MediaType.APPLICATION_JSON
            }
        }

        val httpMethod = HttpMethod.valueOf(request.method)
        val entity = HttpEntity(body, headers)

        return try {
            val downstream = restTemplate.exchange(targetUri, httpMethod, entity, String::class.java)
            // Filter downstream headers — copying them verbatim leaks
            // hop-by-hop headers like `Transfer-Encoding: chunked` which
            // Tomcat then duplicates on its own response, leading to an
            // invalid `chunked,chunked` that NGINX (rightly) rejects.
            ResponseEntity
                .status(downstream.statusCode)
                .headers(passthroughHeaders(downstream.headers))
                .body(downstream.body)
        } catch (e: HttpStatusCodeException) {
            // Surface downstream 4xx/5xx with the original body and status.
            ResponseEntity
                .status(e.statusCode)
                .headers(passthroughHeaders(e.responseHeaders))
                .body(e.responseBodyAsString)
        }
    }

    /** Forwards a small subset of safe response headers from downstream. */
    private fun passthroughHeaders(src: HttpHeaders?): HttpHeaders {
        val out = HttpHeaders()
        if (src == null) return out
        src.contentType?.let { out.contentType = it }
        src[HttpHeaders.LOCATION]?.firstOrNull()?.let { out.set(HttpHeaders.LOCATION, it) }
        return out
    }
}
