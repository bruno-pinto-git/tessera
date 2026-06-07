package com.tessera.ticket.events

import com.tessera.ticket.event.Event
import com.tessera.ticket.event.EventRepository
import com.tessera.ticket.ticket.TicketService
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val CAPTURE_QUEUE = "test.capture.ticket-paid"

/**
 * Producer-side integration test of the event seam: paying a ticket by card
 * must transition it to PAID and publish `ticket.ticket.paid` to the real
 * exchange — crucially *after* the transaction commits (the Transaction
 * synchronization in TicketService.publishOnCommit). The unit tests can only
 * take the no-active-transaction branch, so this is the path they cannot reach.
 *
 * Boots the full ticket-service against Testcontainers Postgres + RabbitMQ and
 * binds a throwaway queue to capture the published message.
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@Import(TicketPaymentEventFlowIntegrationTest.TestQueueConfig::class)
class TicketPaymentEventFlowIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @ServiceConnection
        @JvmStatic
        val rabbit = RabbitMQContainer("rabbitmq:3.13-management-alpine")
    }

    /** Binds a capture queue to the shared exchange for the paid routing key. */
    @TestConfiguration
    class TestQueueConfig {
        @Bean
        fun testPaidQueue(): Queue = Queue(CAPTURE_QUEUE, false)

        @Bean
        fun testPaidBinding(testPaidQueue: Queue, eventsExchange: TopicExchange): Binding =
            BindingBuilder.bind(testPaidQueue).to(eventsExchange).with("ticket.ticket.paid")
    }

    @Autowired private lateinit var ticketService: TicketService
    @Autowired private lateinit var eventRepository: EventRepository
    @Autowired private lateinit var rabbitTemplate: RabbitTemplate

    @Test
    fun `paying a ticket by card publishes ticket-paid after commit`() {
        val event = eventRepository.save(
            Event(
                matchId = 5L,
                name = "Demo",
                priceNormal = BigDecimal("15.00"),
                priceSupporter = BigDecimal("8.00"),
                status = "PUBLISHED",
            ),
        )
        val ticket = ticketService.create(event.id, supporter = false, ownerSub = "owner-sub")

        ticketService.pay(ticket.id, "CARD", null, null)

        val received = rabbitTemplate.receiveAndConvert(CAPTURE_QUEUE, 10_000)
        assertNotNull(received, "expected a ticket.ticket.paid message on the exchange")
        received as TicketPaidEvent
        assertEquals(ticket.id, received.ticketId)
        assertEquals("CARD", received.paymentMethod)
        assertEquals(0, BigDecimal("15.00").compareTo(received.price))
        assertEquals(5L, received.matchId)
    }
}
