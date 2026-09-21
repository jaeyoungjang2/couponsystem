package com.example.coupon.v4.infrastructure.messaging

import com.example.coupon.support.QueueFullException
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@Component
class InMemoryIssuanceQueueV4 {
    private val queue = LinkedBlockingQueue<IssuanceRequestedV4>(CAPACITY)

    fun enqueue(event: IssuanceRequestedV4) {
        if (!queue.offer(event)) {
            println("Ignoring event: $event")
            throw QueueFullException()
        }
        println("Enqueuing queue: $queue")
    }

    fun poll(): IssuanceRequestedV4? = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)

    fun size(): Int = queue.size

    companion object {
        private const val CAPACITY = 10_000
        private const val POLL_TIMEOUT_MS = 100L
    }
}