package com.example.coupon.v4.infrastructure.messaging

import com.example.coupon.domain.CouponRepository
import com.example.coupon.domain.Issuance
import com.example.coupon.domain.IssuanceRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class IssuanceTransactionalWriterV4(
    private val issuanceRepository: IssuanceRepository,
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun insertAndIncrement(event: IssuanceRequestedV4) {
        issuanceRepository.save(
            Issuance(
                userId = event.userId,
                couponId = event.couponId,
                issuedAt = event.issuedAt,
                expiresAt = event.expiresAt,
            )
        )
        couponRepository.incrementIssuedQuantity(event.couponId)
    }
}