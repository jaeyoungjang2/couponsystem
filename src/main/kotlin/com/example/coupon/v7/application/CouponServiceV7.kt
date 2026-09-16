package com.example.coupon.v7.application

import com.example.coupon.dto.CreateCouponRequest
import com.example.coupon.domain.Coupon
import com.example.coupon.domain.CouponRepository
import com.example.coupon.domain.Issuance
import com.example.coupon.support.CouponNotFoundException
import com.example.coupon.support.NotStartedException
import com.example.coupon.v6.infrastructure.messaging.IssuanceRequestProducer
import com.example.coupon.v6.infrastructure.messaging.IssuanceRequested
import com.example.coupon.v7.infrastructure.cache.CacheProperties
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CouponServiceV7(
    private val couponRepository: CouponRepository,
    private val cacheProperties: CacheProperties,
    private val cacheMetrics: CacheMetrics,
    private val couponIssuerV7: CouponIssuerV7,
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
        couponIssuerV7.initStock(coupon.id!!, coupon.totalQuantity)
        return coupon
    }

    // 쿠폰 발급
    @Transactional
    fun issue(couponId: Long, userId: Long): Issuance {
        cacheMetrics.incrementCouponDbRead()
        // db가 로컬에 있어서 조회 속도가 빨라서 임의로 느리게 조금 만들게 함 (100ms)
//        Thread.sleep(cacheProperties.simulatedLoadLatencyMs)
        val coupon = couponRepository.findById(couponId)
            .orElseThrow { CouponNotFoundException() }

        val now = LocalDateTime.now()

        // 쿠폰 발급이 가능한 시간인지 확인
        if (!coupon.isBookingOpen(now)) {
            throw NotStartedException()
        }

        // redis에 사용한 쿠폰 개수 적용
        couponIssuerV7.tryIsusue(couponId, userId)

        val expiresAt = now.plusSeconds(coupon.validityDays.toLong())


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