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
        val targetUri = URI.create(targetUrl)

        val headers = HttpHeaders().apply {
            request.getHeader(HttpHeaders.AUTHORIZATION)?.let {
                set(HttpHeaders.AUTHORIZATION, it)
            }
            request.getHeader(HttpHeaders.CONTENT_TYPE)?.let {
                set(HttpHeaders.CONTENT_TYPE, it)
            } ?: run {
                if (!body.isNullOrEmpty()) contentType = MediaType.APPLICATION_JSON
            }
        }

        val httpMethod = HttpMethod.valueOf(request.method)
        val entity = HttpEntity(body, headers)

        return try {
            val downstream = restTemplate.exchange(targetUri, httpMethod, entity, String::class.java)
            ResponseEntity
                .status(downstream.statusCode)
                .headers(passthroughHeaders(downstream.headers))
                .body(downstream.body)
        } catch (e: HttpStatusCodeException) {
            ResponseEntity
                .status(e.statusCode)
                .headers(passthroughHeaders(e.responseHeaders))
                .body(e.responseBodyAsString)
        }
    }

    private fun passthroughHeaders(src: HttpHeaders?): HttpHeaders {
        val out = HttpHeaders()
        if (src == null) return out
        src.contentType?.let { out.contentType = it }
        src[HttpHeaders.LOCATION]?.firstOrNull()?.let { out.set(HttpHeaders.LOCATION, it) }
        return out
    }
}
