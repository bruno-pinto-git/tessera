package com.tessera.ticket.payments

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue

@Component
class MbwayRelayQueue {

    private val queue = ConcurrentLinkedQueue<MbwayRelayRequest>()

    fun enqueue(request: MbwayRelayRequest) {
        queue.add(request)
    }

    fun drain(): List<MbwayRelayRequest> = generateSequence { queue.poll() }.toList()
}
