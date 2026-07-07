package com.tessera.ticket.payments

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
