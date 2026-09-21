package com.example.coupon.v11.infrastructure.messaging

import com.example.coupon.domain.CouponRepository
import com.example.coupon.domain.Issuance
import com.example.coupon.domain.IssuanceRepository
import com.example.coupon.v6.infrastructure.messaging.IssuanceRequested
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Component

@Component
class IssuanceTransactionalWriterV11(
    private val issuanceRepository: IssuanceRepository,
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun insertAndIncrement(event: IssuanceRequested) {
        if (issuanceRepository.existsByUserIdAndCouponId(event.userId, event.couponId)) return

        issuanceRepository.save(Issuance(
            userId = event.userId,
            couponId = event.couponId,
            issuedAt = event.issuedAt,
            expiresAt = event.expiresAt,
        ))
        couponRepository.incrementIssuedQuantity(event.couponId)
    }

    @Transactional(readOnly = true)
    fun isAlreadyApplied(event: IssuanceRequested): Boolean =
        issuanceRepository.existsByUserIdAndCouponId(event.userId, event.couponId)
}