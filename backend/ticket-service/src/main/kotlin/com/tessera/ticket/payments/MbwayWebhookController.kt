package com.tessera.ticket.payments

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Public endpoint hit by the MB WAY gateway (mock-mbway in dev, SIBS in
 * production) when the customer accepts / declines / lets the payment expire.
 *
 * Public on purpose — the gateway calls us server-to-server, no JWT. In a
 * real SIBS integration, the payload is AES-GCM encrypted and the headers
 * carry the IV + auth tag for verification. The mock sends plain JSON; if we
 * later switch to real SIBS, this controller decrypts before delegating.
 */
@RestController
@RequestMapping("/api/v1/webhooks/mbway")
class MbwayWebhookController(
    private val webhookService: MbwayWebhookService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun handle(@RequestBody payload: MbwayWebhookPayload): ResponseEntity<Void> {
        log.info(
            "MB WAY webhook: transactionID={} status={}",
            payload.transactionID,
            payload.paymentStatus,
        )
        webhookService.handle(payload)
        return ResponseEntity.noContent().build()
    }
}
