package com.tessera.statistics.events

import com.tessera.statistics.sales.TicketSale
import com.tessera.statistics.sales.TicketSaleRepository
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * End-to-end integration test of the asynchronous read-side seam: a domain
 * event published to the real RabbitMQ exchange is routed to the consumer's
 * queue, deserialized, and projected into the Postgres read-side — none of
 * which the mocked EventConsumerTest exercises.
 *
 * Boots the full statistics-service context against Testcontainers Postgres
 * (Flyway-managed schema) + RabbitMQ.
 */
@SpringBootTest
@Testcontainers
class TicketSalesEventFlowIntegrationTest {

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

    @Autowired private lateinit var rabbitTemplate: RabbitTemplate
    @Autowired private lateinit var saleRepo: TicketSaleRepository

    @Value("\${tessera.events.exchange}")
    private lateinit var exchange: String

    @Test
    fun `a ticket-paid event is projected into a sale row`() {
        val ticketId = 7001L
        rabbitTemplate.convertAndSend(
            exchange, "ticket.ticket.paid",
            TicketPaidEvent(
                occurredAt = OffsetDateTime.parse("2026-05-01T19:00:00Z"),
                ticketId = ticketId,
                eventId = 1L,
                matchId = 5L,
                price = BigDecimal("12.50"),
                paymentMethod = "CARD",
                paidAt = OffsetDateTime.parse("2026-05-01T19:00:00Z"),
            ),
        )

        val sale = awaitSale(ticketId)
        assertEquals(5L, sale.matchId)
        assertEquals(0, BigDecimal("12.50").compareTo(sale.price))
        assertEquals("CARD", sale.paymentMethod)
    }

    @Test
    fun `a ticket-validated event stamps validatedAt on the existing sale`() {
        val ticketId = 7002L
        rabbitTemplate.convertAndSend(
            exchange, "ticket.ticket.paid",
            TicketPaidEvent(
                occurredAt = OffsetDateTime.parse("2026-05-01T19:00:00Z"),
                ticketId = ticketId,
                eventId = 1L,
                matchId = 5L,
                price = BigDecimal("10.00"),
                paymentMethod = "MBWAY",
                paidAt = OffsetDateTime.parse("2026-05-01T19:00:00Z"),
            ),
        )
        awaitSale(ticketId)

        val validatedAt = OffsetDateTime.parse("2026-05-01T20:30:00Z")
        rabbitTemplate.convertAndSend(
            exchange, "ticket.ticket.validated",
            TicketValidatedEvent(
                occurredAt = validatedAt,
                ticketId = ticketId,
                matchId = 5L,
                validatedAt = validatedAt,
                validatorSub = "staff-sub",
            ),
        )

        val stamped = awaitCondition(ticketId) { it.validatedAt != null }
        assertNotNull(stamped.validatedAt)
    }

    // Poll the read-side until the projection catches up (the consumer runs
    // asynchronously on its own thread/transaction).
    private fun awaitSale(ticketId: Long): TicketSale = awaitCondition(ticketId) { true }

    private fun awaitCondition(ticketId: Long, predicate: (TicketSale) -> Boolean): TicketSale {
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            val sale = saleRepo.findById(ticketId).orElse(null)
            if (sale != null && predicate(sale)) return sale
            Thread.sleep(200)
        }
        throw AssertionError("read-side projection for ticket $ticketId did not converge within 15s")
    }
}
