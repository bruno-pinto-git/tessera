package com.tessera.ticket.payments

import com.stripe.Stripe
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import com.tessera.ticket.ticket.Ticket
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class StripeGatewayClient(
    @Value("\${tessera.stripe.secret-key:}") secretKey: String,
    @Value("\${tessera.stripe.success-url}") private val successUrlTemplate: String,
    @Value("\${tessera.stripe.cancel-url}") private val cancelUrlTemplate: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        Stripe.apiKey = secretKey
    }

    data class StripeCheckoutInitiation(val sessionId: String, val checkoutUrl: String)

    fun createCheckoutSession(ticket: Ticket): StripeCheckoutInitiation {
        val eventId = ticket.event?.id ?: 0
        val params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setSuccessUrl(fillPlaceholders(successUrlTemplate, ticket.id, eventId))
            .setCancelUrl(fillPlaceholders(cancelUrlTemplate, ticket.id, eventId))
            .putMetadata("ticketId", ticket.id.toString())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("eur")
                            .setUnitAmount(toCents(ticket.price))
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(ticket.event?.name ?: "Bilhete Tessera")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        log.info("Stripe: createCheckoutSession ticket={}", ticket.id)
        val session = Session.create(params)
        log.info("Stripe: createCheckoutSession returned sessionId={}", session.id)
        return StripeCheckoutInitiation(sessionId = session.id, checkoutUrl = session.url)
    }

    fun checkStatus(sessionId: String): String {
        val session = Session.retrieve(sessionId)
        log.debug("Stripe: status sessionId={} paymentStatus={}", sessionId, session.paymentStatus)
        return session.paymentStatus
    }

    private fun fillPlaceholders(template: String, ticketId: Long, eventId: Long): String =
        template.replace("{id}", ticketId.toString()).replace("{eventId}", eventId.toString())

    private fun toCents(price: BigDecimal): Long =
        price.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).toLong()
}
