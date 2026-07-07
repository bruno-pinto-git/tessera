package com.tessera.ticket.payments

import com.tessera.ticket.ticket.Ticket
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MbwayGatewayClient(
    private val relayQueue: MbwayRelayQueue,
    @Value("\${tessera.mbway.webhook-base-url}") private val webhookBaseUrl: String,
    @Value("\${tessera.mbway.terminal-id}") private val terminalId: Int,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun initiatePayment(ticket: Ticket, customerPhone: String): String {
        val transactionId = UUID.randomUUID().toString().replace("-", "")
        relayQueue.enqueue(
            MbwayRelayRequest(
                transactionID = transactionId,
                transactionSignature = UUID.randomUUID().toString().replace("-", ""),
                merchantTransactionId = "ticket-${ticket.id}",
                terminalId = terminalId,
                description = ticket.event?.name ?: "Bilhete Tessera",
                amount = Amount(value = ticket.price.toDouble(), currency = "EUR"),
                customerPhone = customerPhone,
                callbackUrl = "$webhookBaseUrl/api/v1/webhooks/mbway",
            ),
        )
        log.info("MB WAY: queued relay request transactionID={} ticket={}", transactionId, ticket.id)
        return transactionId
    }
}
