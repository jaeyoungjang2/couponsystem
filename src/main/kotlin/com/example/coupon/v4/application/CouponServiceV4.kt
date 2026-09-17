package com.example.coupon.v4.application

import com.example.coupon.dto.CreateCouponRequest
import com.example.coupon.domain.Coupon
import com.example.coupon.domain.CouponRepository
import com.example.coupon.domain.Issuance
import com.example.coupon.support.CouponNotFoundException
import com.example.coupon.support.NotStartedException
import com.example.coupon.v4.infrastructure.messaging.InMemoryIssuanceQueueV4
import com.example.coupon.v4.infrastructure.messaging.IssuanceRequestedV4
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class CouponServiceV4(
    private val couponRepository: CouponRepository,
    private val couponIssuerV4: CouponIssuerV4,
    private val issuanceQueue: InMemoryIssuanceQueueV4,
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
        couponIssuerV4.initStock(coupon.id!!, coupon.totalQuantity)
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
        couponIssuerV4.tryIssue(couponId, userId)

        val expiresAt = now.plusSeconds(coupon.validityDays.toLong())


        issuanceQueue.enqueue(
            IssuanceRequestedV4(
                couponId = couponId,
                userId = userId,
                issuedAt = now,
                expiresAt = expiresAt,
            )
        )
        println("enqueued issuance")

        return Issuance(
            userId = userId,
            couponId = couponId,
            issuedAt = now,
            expiresAt = expiresAt,
        )
    }
}