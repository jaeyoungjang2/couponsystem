package com.example.coupon.v5.application

import com.example.coupon.dto.CreateCouponRequest
import com.example.coupon.domain.Coupon
import com.example.coupon.domain.CouponRepository
import com.example.coupon.domain.Issuance
import com.example.coupon.support.CouponNotFoundException
import com.example.coupon.support.NotStartedException
import com.example.coupon.v5.infrastructure.messaging.IssuanceRequestedV5
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CouponServiceV5(
    private val couponRepository: CouponRepository,
    private val couponIssuerV5: CouponIssuerV5,
    private val eventPublisher: ApplicationEventPublisher
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
        couponIssuerV5.initStock(coupon.id!!, coupon.totalQuantity)
        return coupon
    }

    // 쿠폰 발급
    @Transactional
    fun issue(couponId: Long, userId: Long): Issuance {
        // 존재하는 쿠폰인지 확인
        // v2에서 비관적 락을 적용하던 부분을 개선하였다.
        // 비관적 락으로 인해서 발생하던 락이 걸리는 임계 영역을 redis를 이용해서 범위를 줄임 -> 아직 따닥 문제가 발생하는 상황
        val coupon = couponRepository.findById(couponId)
            .orElseThrow { CouponNotFoundException() }

        val now = LocalDateTime.now()

        // 쿠폰 발급이 가능한 시간인지 확인
        if (!coupon.isBookingOpen(now)) {
            throw NotStartedException()
        }

        // redis에 사용한 쿠폰 개수 적용
        couponIssuerV5.tryIssue(couponId, userId)

        val expiresAt = now.plusSeconds(coupon.validityDays.toLong())


        eventPublisher.publishEvent(
            IssuanceRequestedV5(
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