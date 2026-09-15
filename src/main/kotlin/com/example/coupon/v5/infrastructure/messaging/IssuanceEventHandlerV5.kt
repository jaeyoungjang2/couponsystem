package com.example.coupon.v5.infrastructure.messaging

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async

class IssuanceEventHandlerV5(
    private val writer: IssuanceWriterV5,
) {
    @Async(ISSUANCE_TASK_EXECUTOR)
    @EventListener
    fun handle(event: IssuanceRequestedV5) {
        writer.write(event)
    }
}