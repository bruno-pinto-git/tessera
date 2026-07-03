package com.tessera.ticket.events

import com.tessera.ticket.event.Event
import com.tessera.ticket.event.EventRepository
import com.tessera.ticket.ticket.Ticket
import com.tessera.ticket.ticket.TicketRepository
import com.tessera.ticket.ticket.TicketService
import com.tessera.ticket.ticket.TicketStatus
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val CAPTURE_QUEUE = "test.capture.ticket-validated"

/**
 * Producer-side integration test of the event seam: validating a ticket must
 * transition it to VALIDATED and publish `ticket.ticket.validated` to the
 * real exchange — crucially *after* the transaction commits (the transaction
 * synchronization in TicketService.publishOnCommit). The unit tests can only
 * take the no-active-transaction branch, so this is the path they cannot
 * reach.
 *
 * `validate()` is used here rather than `pay()`: both payment methods are now
 * asynchronous (MBWAY needs a real gateway/phone, CARD needs a real Stripe
 * account), so there is no synchronous PAID transition reachable through
 * `pay()` without a live external dependency. `validate()` exercises the
 * exact same @Transactional + publishOnCommit mechanics with none of that —
 * the PAID precondition is set up directly via the repository.
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

    /** Binds a capture queue to the shared exchange for the validated routing key. */
    @TestConfiguration
    class TestQueueConfig {
        @Bean
        fun testValidatedQueue(): Queue = Queue(CAPTURE_QUEUE, false)

        @Bean
        fun testValidatedBinding(testValidatedQueue: Queue, eventsExchange: TopicExchange): Binding =
            BindingBuilder.bind(testValidatedQueue).to(eventsExchange).with("ticket.ticket.validated")
    }

    @Autowired private lateinit var ticketService: TicketService
    @Autowired private lateinit var ticketRepository: TicketRepository
    @Autowired private lateinit var eventRepository: EventRepository
    @Autowired private lateinit var rabbitTemplate: RabbitTemplate

    @Test
    fun `validating a ticket publishes ticket-validated after commit`() {
        val event = eventRepository.save(
            Event(
                matchId = null,
                name = "Demo",
                priceNormal = BigDecimal("15.00"),
                priceSupporter = BigDecimal("8.00"),
                status = "PUBLISHED",
            ),
        )
        val created: Ticket = ticketService.create(event.id, supporter = false, ownerSub = "owner-sub")
        // Both payment methods need a live external gateway (an MB WAY phone,
        // a real Stripe account) to reach PAID through pay() — set up the
        // precondition directly via the repository instead; what's under test
        // here is validate()'s publish-after-commit behaviour, not how a
        // ticket gets to PAID.
        created.status = TicketStatus.PAID
        created.paymentDate = OffsetDateTime.now(ZoneOffset.UTC)
        created.paymentMethod = "CARD"
        val paid = ticketRepository.save(created)

        ticketService.validate(paid.code, "staff-sub", isPlatformAdmin = true, staffClubIds = emptySet())

        val received = rabbitTemplate.receiveAndConvert(CAPTURE_QUEUE, 10_000)
        assertNotNull(received, "expected a ticket.ticket.validated message on the exchange")
        received as TicketValidatedEvent
        assertEquals(paid.id, received.ticketId)
    }
}
