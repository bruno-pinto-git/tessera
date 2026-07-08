package com.tessera.bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig(
    // Comma-separated allowed origins. Defaults to the local dev hosts; the prod
    // deploy overlay sets APP_CORS_ALLOWED_ORIGINS to the public site origin
    // (https://<fqdn>) so browser writes (POST/PUT/DELETE) — which carry an Origin
    // header even same-origin — aren't rejected with "Invalid CORS request".
    @Value("\${app.cors.allowed-origins:http://localhost:5173,http://localhost:8000}")
    private val allowedOrigins: String,
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(*allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
    }
}
