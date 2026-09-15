package com.example.coupon.v4.infrastructure.messaging

import java.time.LocalDateTime

class IssuanceRequestedV4(
    val couponId: Long,
    val userId: Long,
    val issuedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
) {
}