package com.example.coupon.application

import com.example.coupon.api.dto.CreateCouponRequest
import com.example.coupon.domain.Coupon
import com.example.coupon.domain.CouponRepository
import com.example.coupon.domain.Issuance
import com.example.coupon.domain.IssuanceRepository
import com.example.coupon.support.AlreadyIssuedException
import com.example.coupon.support.CouponNotFoundException
import com.example.coupon.support.NotStartedException
import com.example.coupon.support.SoldOutException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CouponService(
    val couponRepository: CouponRepository,
    val issuanceRepository: IssuanceRepository,
) {
    @Transactional
    fun createCoupon(request: CreateCouponRequest): Coupon {
        val coupon = Coupon(
            name = request.name,
            totalQuantity = request.totalQuantity,
            validityDays = request.validityDays,
            startsAt = request.startsAt,
        )
        return couponRepository.save(coupon)
    }

    // 쿠폰 유/무 검증
    @Transactional
    fun issue(couponId: Long, userId: Long): Issuance {
        val coupon = couponRepository.findById(couponId)
            .orElseThrow { CouponNotFoundException() }

        val now = LocalDateTime.now()

        // 쿠폰 발행이 가능한 시간인지 확인
        if (!coupon.isBookingOpen(now)) {
            throw NotStartedException()
        }

        // 매진된 쿠폰인지 확인
        if (coupon.isSoldOut()) {
            throw SoldOutException()
        }

        // 이미 특정 사용자에게 발행된 적이 있는지 확인
        if (issuanceRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw AlreadyIssuedException()
        }

        coupon.issuedQuantity++

        return issuanceRepository.save(
            Issuance(
                userId = userId,
                couponId = couponId,
                issuedAt = now,
                expiresAt = now.plusDays(coupon.validityDays.toLong()),
            )
        )
    }
}