package com.example.coupon.v11.api

import com.example.coupon.domain.Coupon
import com.example.coupon.domain.CouponRepository
import com.example.coupon.domain.Issuance
import com.example.coupon.dto.CreateCouponRequest
import com.example.coupon.support.NotStartedException
import com.example.coupon.support.SoldOutException
import com.example.coupon.v11.application.CouponIssuePolicyReaderV11
import com.example.coupon.v11.application.CouponIssuerV11
import com.example.coupon.v11.application.SoldOutState
import com.example.coupon.v6.infrastructure.messaging.IssuanceRequestProducer
import com.example.coupon.v6.infrastructure.messaging.IssuanceRequested
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CouponServiceV11(
    private val couponRepository: CouponRepository,
    private val couponIssuePolicyReader: CouponIssuePolicyReaderV11,
    private val couponIssuer: CouponIssuerV11,
    private val issuanceRequestProducer: IssuanceRequestProducer,
    private val soldOutState: SoldOutState,
) {

    @Transactional
    fun createCoupon(request: CreateCouponRequest): Coupon {
        val coupon = couponRepository.save(
            Coupon(
                name = request.name,
                totalQuantity = request.totalQuantity,
                validityDays = request.validityDays,
                startsAt = request.startsAt,
            )
        )
        couponIssuer.initStock(coupon.id!!, coupon.totalQuantity)
        return coupon
    }

    fun issue(couponId: Long, userId: Long): Issuance {
        if (soldOutState.isSoldOut(couponId)) {
            throw SoldOutException()
        }

        val policy = couponIssuePolicyReader.get(couponId)

        val now = LocalDateTime.now()
        if (!policy.isBookingOpen(now)) {
            throw NotStartedException()
        }

        couponIssuer.tryIssue(couponId, userId)

        val expiresAt = now.plusDays(policy.validityDays.toLong())
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