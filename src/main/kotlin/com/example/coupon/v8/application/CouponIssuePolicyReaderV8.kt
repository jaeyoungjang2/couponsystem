package com.example.coupon.v8.application

import com.example.coupon.common.CouponIssuePolicy
import com.example.coupon.domain.CouponRepository
import com.example.coupon.support.CouponNotFoundException
import com.example.coupon.v8.infrastructure.cache.CouponCacheRepositoryV8
import org.springframework.stereotype.Service

@Service
class CouponIssuePolicyReaderV8(
    private val couponRepository: CouponRepository,
    private val couponCacheRepository: CouponCacheRepositoryV8,
) {

    fun get(id: Long): CouponIssuePolicy = couponCacheRepository.getIssuePolicyOrLoad(id) {
        couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
            .let (CouponIssuePolicy::from)
    }
}