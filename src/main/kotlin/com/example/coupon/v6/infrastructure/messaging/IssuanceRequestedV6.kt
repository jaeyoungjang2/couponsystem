package com.example.coupon.v6.infrastructure.messaging

import java.time.LocalDateTime

class IssuanceRequestedV6(
    val couponId: Long,
    val userId: Long,
    val issuedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
) {
}