package com.tessera.bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
