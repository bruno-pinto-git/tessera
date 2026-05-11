package com.tessera.statistics.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler

/**
 * Stateless OAuth2 resource server.
 * Read endpoints under /api/v1/stats are public; the few admin-only stats
 * (e.g. sales summaries) are guarded via @PreAuthorize on the controller.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val mapper: ObjectMapper,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { /* keep CorsConfig */ }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // Reads on match-sheet history are public (fans browsing history).
                auth.requestMatchers(HttpMethod.GET, "/api/v1/stats/match-sheets/**").permitAll()
                // Sales endpoints are admin-only — declared via @PreAuthorize.
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth ->
                oauth.jwt { it.jwtAuthenticationConverter(jwtAuthConverter()) }
                oauth.authenticationEntryPoint(problemAuthEntryPoint())
                oauth.accessDeniedHandler(problemAccessDeniedHandler())
            }
            .exceptionHandling { eh ->
                eh.authenticationEntryPoint(problemAuthEntryPoint())
                eh.accessDeniedHandler(problemAccessDeniedHandler())
            }
        return http.build()
    }

    private fun jwtAuthConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter(realmRolesConverter())
        return converter
    }

    private fun realmRolesConverter(): Converter<Jwt, Collection<GrantedAuthority>> =
        Converter { jwt ->
            val rolesClaim = jwt.getClaimAsStringList("roles")
                ?: jwt.getClaim<Map<String, Any>?>("realm_access")
                    ?.get("roles")
                    ?.let { @Suppress("UNCHECKED_CAST") (it as List<String>) }
                ?: emptyList()
            rolesClaim
                .filter { it in TESSERA_ROLES }
                .map { SimpleGrantedAuthority("ROLE_$it") }
        }

    private fun problemAuthEntryPoint() =
        AuthenticationEntryPoint { req, res, ex -> writeProblem(req, res,
            HttpStatus.UNAUTHORIZED, "https://tessera/api/errors/unauthorized",
            "Unauthorized", ex.message ?: "Authentication required.") }

    private fun problemAccessDeniedHandler() =
        AccessDeniedHandler { req, res, ex -> writeProblem(req, res,
            HttpStatus.FORBIDDEN, "https://tessera/api/errors/forbidden",
            "Forbidden", ex.message ?: "Insufficient permissions.") }

    private fun writeProblem(
        req: HttpServletRequest,
        res: HttpServletResponse,
        status: HttpStatus,
        type: String,
        title: String,
        detail: String,
    ) {
        res.status = status.value()
        res.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        mapper.writeValue(res.outputStream, mapOf(
            "type"     to type,
            "title"    to title,
            "status"   to status.value(),
            "detail"   to detail,
            "instance" to req.requestURI,
        ))
    }

    companion object {
        private val TESSERA_ROLES = setOf("admin", "staff", "fan")
    }
}
