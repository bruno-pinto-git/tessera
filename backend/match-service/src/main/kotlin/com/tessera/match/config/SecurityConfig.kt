package com.tessera.match.config

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler

/**
 * Stateless OAuth2 resource server. JWT signature is verified against Keycloak
 * via the JWKS endpoint configured in application.yml. We translate the custom
 * `roles` claim (populated by our Keycloak mapper) into Spring Security
 * authorities prefixed with `ROLE_` so that `@PreAuthorize("hasRole('admin')")`
 * works as expected.
 *
 * Read access (GET) on most resources is public. Writes require the appropriate
 * role, declared per-method via `@PreAuthorize`.
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
            .cors { /* keep our CorsConfig */ }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                // Public read endpoints
                auth.requestMatchers(HttpMethod.GET, "/api/v1/clubs/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/v1/venues/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/v1/teams/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/v1/players/**").permitAll()
                auth.requestMatchers(HttpMethod.GET, "/api/v1/matches/**").permitAll()
                // Anything else needs authentication; method-level @PreAuthorize
                // narrows down to specific roles.
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth ->
                oauth.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
                // Filter-stage failures (no token / bad token) bypass
                // @RestControllerAdvice. Wire custom handlers so they still
                // produce RFC 7807 Problem JSON.
                oauth.authenticationEntryPoint(problemAuthenticationEntryPoint())
                oauth.accessDeniedHandler(problemAccessDeniedHandler())
            }
            .exceptionHandling { eh ->
                eh.authenticationEntryPoint(problemAuthenticationEntryPoint())
                eh.accessDeniedHandler(problemAccessDeniedHandler())
            }
        return http.build()
    }

    private fun problemAuthenticationEntryPoint() =
        AuthenticationEntryPoint { req, res, ex -> writeProblem(req, res, HttpStatus.UNAUTHORIZED,
            "https://tessera/api/errors/unauthorized", "Unauthorized",
            ex.message ?: "Authentication required.") }

    private fun problemAccessDeniedHandler() =
        AccessDeniedHandler { req, res, ex -> writeProblem(req, res, HttpStatus.FORBIDDEN,
            "https://tessera/api/errors/forbidden", "Forbidden",
            ex.message ?: "Insufficient permissions.") }

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
        val body = mapOf(
            "type"     to type,
            "title"    to title,
            "status"   to status.value(),
            "detail"   to detail,
            "instance" to req.requestURI,
        )
        mapper.writeValue(res.outputStream, body)
    }

    /**
     * Maps JWT claims to Spring Security authorities.
     *
     * Token shape (from our Keycloak realm):
     *   {
     *     "roles": ["admin", "default-roles-tessera"],
     *     ...
     *   }
     *
     * We expose the realm-level roles as `ROLE_admin`, `ROLE_staff`, `ROLE_fan`.
     */
    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
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

    companion object {
        private val TESSERA_ROLES = setOf("admin", "staff", "fan")
    }
}
