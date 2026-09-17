package com.example.coupon.v8.application

import com.example.coupon.common.CacheMetrics
import com.example.coupon.domain.Coupon
import com.example.coupon.domain.CouponRepository
import com.example.coupon.domain.Issuance
import com.example.coupon.dto.CreateCouponRequest
import com.example.coupon.support.NotStartedException
import com.example.coupon.v6.infrastructure.messaging.IssuanceRequestProducer
import com.example.coupon.v6.infrastructure.messaging.IssuanceRequested
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CouponServiceV8(
    private val couponRepository: CouponRepository,
    private val couponIssuePolicyReader: CouponIssuePolicyReaderV8,
    private val cacheMetrics: CacheMetrics,
    private val couponIssuerV8: CouponIssuerV8,
    private val issuanceRequestProducer: IssuanceRequestProducer
) {
    // 쿠폰 생성
    @Transactional
    fun createCoupon(request: CreateCouponRequest): Coupon {

        val coupon = couponRepository.save(Coupon(
            name = request.name,
            totalQuantity = request.totalQuantity,
            validityDays = request.validityDays,
            startsAt = request.startsAt,
        ))
        couponIssuerV8.initStock(coupon.id!!, coupon.totalQuantity)
        return coupon
    }

    // 쿠폰 발급
    @Transactional
    fun issue(couponId: Long, userId: Long): Issuance {
        val policy = couponIssuePolicyReader.get(couponId)

        val now = LocalDateTime.now()

        // 쿠폰 발급이 가능한 시간인지 확인
        if (!policy.isBookingOpen(now)) {
            throw NotStartedException()
        }

        // redis에 사용한 쿠폰 개수 적용
        couponIssuerV8.tryIsusue(couponId, userId)

        val expiresAt = now.plusSeconds(policy.validityDays.toLong())


        issuanceRequestProducer.publish(
            IssuanceRequested(
                couponId = couponId,
                userId = userId,
                issuedAt = now,
                expiresAt = expiresAt,
            )
        )

        return Issuance(
            userId = userId,
            couponId = couponId,
            issuedAt = now,
            expiresAt = expiresAt,
        )
    }
}