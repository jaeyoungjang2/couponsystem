package com.example.coupon.v6.infrastructure.messaging

import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class IssuanceRequestProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun publish(event: IssuanceRequestedV6) {
        // userId를 kafka key로 전송
        kafkaTemplate.send(IssuanceTopics.REQUESTED, event.userId.toString(), event)
    }
}