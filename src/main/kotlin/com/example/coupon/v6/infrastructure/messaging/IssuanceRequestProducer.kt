package com.example.coupon.v6.infrastructure.messaging

import com.example.coupon.v6.infrastructure.messaging.IssuanceTopics
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class IssuanceRequestProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {
    fun publish(event: IssuanceRequested) {
        // userId를 kafka key로 전송
        kafkaTemplate.send(IssuanceTopics.REQUESTED, event.userId.toString(), event)
    }

    fun publishAndWait(event: IssuanceRequested) {
        kafkaTemplate.send(IssuanceTopics.REQUESTED, event.userId.toString(), event)
            .get(10, TimeUnit.SECONDS)
    }
}