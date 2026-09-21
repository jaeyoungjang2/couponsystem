package com.example.coupon.v6.infrastructure.messaging

import java.time.LocalDateTime

class IssuanceRequested(
    val couponId: Long,
    val userId: Long,
    val issuedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
) {
}