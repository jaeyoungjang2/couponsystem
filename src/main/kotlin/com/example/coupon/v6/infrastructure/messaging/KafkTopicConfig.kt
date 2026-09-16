package com.example.coupon.v6.infrastructure.messaging

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaTopicConfig {

    // 2개의 topic 생성
    @Bean
    fun issuanceRequestedTopic(): NewTopic =
        TopicBuilder.name(IssuanceTopics.REQUESTED).partitions(3).replicas(1).build()

    @Bean
    fun issuanceRequestedDltTopic(): NewTopic =
        TopicBuilder.name(IssuanceTopics.REQUESTED_DLT).partitions(3).replicas(1).build()
}