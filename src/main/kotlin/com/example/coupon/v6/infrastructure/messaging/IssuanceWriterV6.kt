package com.example.coupon.v6.infrastructure.messaging

import com.example.coupon.v11.infrastructure.messaging.IssuanceTransactionalWriterV11
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class IssuanceWriter(
    private val issuanceTransactionalWriter: IssuanceTransactionalWriterV11,
) {
    fun write(event: IssuanceRequested) {
        try {
            issuanceTransactionalWriter.insertAndIncrement(event)
        } catch (e: DataIntegrityViolationException) {
            if (!issuanceTransactionalWriter.isAlreadyApplied(event)) throw e
            log.debug { "이미 저장된 발급은 멱등 처리: couponId=${event.couponId}, userId=${event.userId}" }
        }
    }
}