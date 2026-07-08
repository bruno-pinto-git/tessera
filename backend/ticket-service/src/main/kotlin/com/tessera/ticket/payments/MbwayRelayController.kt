package com.tessera.ticket.payments

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Polled by the mock-mbway app to pick up pending MBWAY payments — the
 * backend can no longer call the phone directly once it's deployed on a
 * network the phone isn't on, so the phone pulls instead.
 */
@RestController
@RequestMapping("/api/v1/mbway/relay")
class MbwayRelayController(
    private val relayQueue: MbwayRelayQueue,
    @Value("\${tessera.mbway.relay-secret:}") private val expectedSecret: String,
) {

    @PostMapping("/poll")
    fun poll(@RequestHeader("X-Relay-Secret") secret: String): List<MbwayRelayRequest> {
        if (secret != expectedSecret) {
            throw AccessDeniedException("Invalid relay secret")
        }
        return relayQueue.drain()
    }
}
