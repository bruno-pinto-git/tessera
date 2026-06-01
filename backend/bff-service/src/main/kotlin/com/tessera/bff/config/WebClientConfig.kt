package com.tessera.bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

@Configuration
class WebClientConfig {

    @Bean("ticketServiceUrl")
    fun ticketServiceUrl(
        @Value("\${services.ticket-url}") ticketUrl: String
    ): String = ticketUrl

    @Bean("matchServiceUrl")
    fun matchServiceUrl(
        @Value("\${services.match-url}") matchUrl: String
    ): String = matchUrl

    @Bean("statisticsServiceUrl")
    fun statisticsServiceUrl(
        @Value("\${services.statistics-url}") statisticsUrl: String
    ): String = statisticsUrl

    /**
     * Uses Apache HttpClient 5 (via [HttpComponentsClientHttpRequestFactory])
     * instead of the default `SimpleClientHttpRequestFactory`. The default is
     * backed by `HttpURLConnection`, which CANNOT send `PATCH` requests
     * (throws `ProtocolException: Invalid HTTP method: PATCH`). Since the BFF
     * proxies `PATCH` edits (matches, teams, players, clubs, venues) to the
     * downstream services, it must use a factory that supports PATCH.
     */
    @Bean
    fun restTemplate(): RestTemplate = RestTemplate(HttpComponentsClientHttpRequestFactory())
}
