package com.example.coupon.v6.infrastructure.messaging

import com.example.coupon.v6.infrastructure.messaging.IssuanceTopics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class IssuanceWorker(
    private val writer: IssuanceWriterV6,
) {
    @KafkaListener(
        topics = [IssuanceTopics.REQUESTED],
        groupId = IssuanceTopics.CONSUMER_GROUP,
        concurrency = "3",
    )
    fun consume(event: IssuanceRequested) {
        writer.write(event)
    }
}