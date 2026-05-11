package com.tessera.ticket.events

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * AMQP setup for ticket-service as a *producer*. We declare the shared
 * `tessera.events` topic exchange so it exists when the first message is
 * published, regardless of which service starts first. Consumers declare
 * their own queues bound to this exchange.
 */
@Configuration
class RabbitConfig {

    @Bean
    fun eventsExchange(
        @Value("\${tessera.events.exchange}") name: String,
    ): TopicExchange = TopicExchange(name, /* durable */ true, /* autoDelete */ false)

    @Bean
    fun jacksonMessageConverter(mapper: ObjectMapper): Jackson2JsonMessageConverter =
        Jackson2JsonMessageConverter(mapper)

    @Bean
    fun rabbitTemplate(
        connectionFactory: ConnectionFactory,
        converter: Jackson2JsonMessageConverter,
    ): RabbitTemplate = RabbitTemplate(connectionFactory).apply {
        messageConverter = converter
    }
}
