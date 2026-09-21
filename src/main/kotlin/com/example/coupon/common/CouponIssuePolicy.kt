package com.example.coupon.common

import com.example.coupon.domain.Coupon
import java.time.LocalDateTime

class CouponIssuePolicy(
    val startsAt: LocalDateTime?,
    val validityDays: Int,
) {
    fun isBookingOpen(now: LocalDateTime): Boolean =
        startsAt?.let { !now.isBefore(it) } ?: true

    // static method
    companion object {
        fun from(coupon: Coupon): CouponIssuePolicy = CouponIssuePolicy(
            startsAt = coupon.startsAt,
            validityDays = coupon.validityDays,
        )
    }
}