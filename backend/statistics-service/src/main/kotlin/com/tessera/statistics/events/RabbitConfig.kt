package com.tessera.statistics.events

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Consumer-side AMQP topology for statistics-service.
 *
 * We declare:
 *   - the shared `tessera.events` topic exchange (idempotent if the producer
 *     already declared it)
 *   - one durable queue per event type we care about
 *   - the bindings between queue and exchange
 *
 * Consumers are wired via @RabbitListener with the queue names from
 * application.yml.
 */
@Configuration
class RabbitConfig {

    @Bean
    fun eventsExchange(
        @Value("\${tessera.events.exchange}") name: String,
    ): TopicExchange = TopicExchange(name, /* durable */ true, /* autoDelete */ false)

    @Bean
    fun matchSheetClosedQueue(
        @Value("\${tessera.events.match-sheet-closed-queue}") name: String,
    ): Queue = Queue(name, /* durable */ true)

    @Bean
    fun matchSheetClosedBinding(
        matchSheetClosedQueue: Queue,
        eventsExchange: TopicExchange,
    ): Binding = BindingBuilder
        .bind(matchSheetClosedQueue)
        .to(eventsExchange)
        .with("match.sheet.closed")

    @Bean
    fun ticketPaidQueue(
        @Value("\${tessera.events.ticket-paid-queue}") name: String,
    ): Queue = Queue(name, /* durable */ true)

    @Bean
    fun ticketPaidBinding(
        ticketPaidQueue: Queue,
        eventsExchange: TopicExchange,
    ): Binding = BindingBuilder
        .bind(ticketPaidQueue)
        .to(eventsExchange)
        .with("ticket.ticket.paid")

    @Bean
    fun ticketValidatedQueue(
        @Value("\${tessera.events.ticket-validated-queue}") name: String,
    ): Queue = Queue(name, /* durable */ true)

    @Bean
    fun ticketValidatedBinding(
        ticketValidatedQueue: Queue,
        eventsExchange: TopicExchange,
    ): Binding = BindingBuilder
        .bind(ticketValidatedQueue)
        .to(eventsExchange)
        .with("ticket.ticket.validated")

    @Bean
    fun jacksonMessageConverter(mapper: ObjectMapper): MessageConverter =
        Jackson2JsonMessageConverter(mapper)
}
