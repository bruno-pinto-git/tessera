package com.tessera.ticket.payments

import com.tessera.ticket.ticket.Ticket
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * HTTP client for the MB WAY payment gateway. Talks the SIBS protocol
 * (mock-mbway in dev, real SIBS in production — selected via
 * `tessera.mbway.gateway-url`).
 *
 * The flow is the two-step SIBS dance:
 *   1. POST /api/v1/payments              → returns transactionID + signature
 *   2. POST /api/v1/payments/{id}/mbway/purchase {customerPhone}
 *                                          → returns paymentStatus=Pending
 *
 * After step 2, the gateway will eventually call our webhook
 * (configured via `merchant.callbackUrl`) when the customer accepts /
 * declines / the request expires.
 */
@Component
class MbwayGatewayClient(
    @Value("\${tessera.mbway.gateway-url}") private val gatewayUrl: String,
    @Value("\${tessera.mbway.webhook-base-url}") private val webhookBaseUrl: String,
    @Value("\${tessera.mbway.terminal-id}") private val terminalId: Int,
    @Value("\${tessera.mbway.client-id:tessera-mock}") private val clientId: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    /**
     * Initiates a MB WAY payment for [ticket] with the given customer phone.
     * Returns the gateway-side `transactionID` (caller stores on the ticket so
     * the webhook can correlate).
     */
    fun initiatePayment(ticket: Ticket, customerPhone: String): String {
        val checkout = createCheckout(ticket)
        triggerPurchase(checkout.transactionID, checkout.transactionSignature, customerPhone)
        return checkout.transactionID
    }

    private fun createCheckout(ticket: Ticket): CreatePaymentResponse {
        val body = CreatePaymentRequest(
            merchant = MerchantIn(
                terminalId = terminalId,
                channel = "web",
                merchantTransactionId = "ticket-${ticket.id}",
                transactionDescription = ticket.event?.name ?: "Tessera",
                callbackUrl = "$webhookBaseUrl/api/v1/webhooks/mbway",
            ),
            transaction = TransactionIn(
                transactionTimestamp = OffsetDateTime.now(ZoneOffset.UTC).toString(),
                description = ticket.event?.name ?: "Bilhete Tessera",
                moto = false,
                paymentType = "PURS",
                paymentMethod = listOf("MBWAY"),
                amount = Amount(
                    value = ticket.price.toDouble(),
                    currency = "EUR",
                ),
            ),
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", "Bearer tessera-mock-token")
            set("X-IBM-Client-Id", clientId)
        }
        val url = "$gatewayUrl/api/v1/payments"
        log.info("MB WAY: createCheckout {} ticket={}", url, ticket.id)
        val response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(body, headers),
            CreatePaymentResponse::class.java,
        ).body ?: error("MB WAY gateway returned empty body on createCheckout")
        log.info("MB WAY: createCheckout returned transactionID={}", response.transactionID)
        return response
    }

    private fun triggerPurchase(
        transactionId: String,
        transactionSignature: String,
        customerPhone: String,
    ) {
        val body = MbwayPurchaseRequest(customerPhone = customerPhone)
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("Authorization", transactionSignature)
            set("X-IBM-Client-Id", clientId)
        }
        val url = "$gatewayUrl/api/v1/payments/$transactionId/mbway/purchase"
        log.info("MB WAY: triggerPurchase {} phone={}", url, customerPhone)
        val response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            HttpEntity(body, headers),
            MbwayPurchaseResponse::class.java,
        ).body ?: error("MB WAY gateway returned empty body on purchase")
        log.info("MB WAY: triggerPurchase returned paymentStatus={}", response.paymentStatus)
    }
}
