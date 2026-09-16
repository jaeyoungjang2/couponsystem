package com.example.coupon.v6.infrastructure.messaging

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class IssuanceWorkerV6(
    private val writer: IssuanceWriterV6,
) {
    @KafkaListener(
        topics = [IssuanceTopics.REQUESTED],
        groupId = IssuanceTopics.CONSUMER_GROUP,
        concurrency = "3",
    )
    fun consume(event: IssuanceRequestedV6) {
        writer.write(event)
    }
}