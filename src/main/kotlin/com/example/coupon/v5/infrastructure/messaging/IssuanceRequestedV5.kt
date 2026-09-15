package com.example.coupon.v5.infrastructure.messaging

import java.time.LocalDateTime

class IssuanceRequestedV5(
    val couponId: Long,
    val userId: Long,
    val issuedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
) {
}