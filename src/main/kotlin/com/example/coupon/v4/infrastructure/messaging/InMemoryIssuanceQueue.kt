package com.example.coupon.v4.infrastructure.messaging

import com.example.coupon.support.QueueFullException
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@Component
class InMemoryIssuanceQueue {
    private val queue = LinkedBlockingQueue<IssuanceRequested>(CAPACITY)

    fun enqueue(event: IssuanceRequested) {
        if (!queue.offer(event)) {
            println("Ignoring event: $event")
            throw QueueFullException()
        }
        println("Enqueuing queue: $queue")
    }

    fun poll(): IssuanceRequested? = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)

    fun size(): Int = queue.size

    companion object {
        private const val CAPACITY = 10_000
        private const val POLL_TIMEOUT_MS = 100L
    }
}