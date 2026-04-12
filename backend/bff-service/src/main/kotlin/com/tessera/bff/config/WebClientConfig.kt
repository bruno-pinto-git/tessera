package com.tessera.bff.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    fun ticketServiceClient(
        @Value("\${services.ticket-url}") ticketUrl: String
    ): WebClient = WebClient.builder()
        .baseUrl(ticketUrl)
        .build()

    @Bean
    fun matchServiceClient(
        @Value("\${services.match-url}") matchUrl: String
    ): WebClient = WebClient.builder()
        .baseUrl(matchUrl)
        .build()

    @Bean
    fun statisticsServiceClient(
        @Value("\${services.statistics-url}") statisticsUrl: String
    ): WebClient = WebClient.builder()
        .baseUrl(statisticsUrl)
        .build()
}
