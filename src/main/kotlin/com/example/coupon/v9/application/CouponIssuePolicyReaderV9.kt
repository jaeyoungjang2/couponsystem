package com.example.coupon.v9.application

import com.example.coupon.common.CouponIssuePolicy
import com.example.coupon.domain.CouponRepository
import com.example.coupon.support.CouponNotFoundException
import com.example.coupon.v9.infrastructure.cache.CouponCacheRepositoryV9
import org.springframework.stereotype.Service

@Service
class CouponIssuePolicyReaderV9(
    private val couponRepository: CouponRepository,
    private val couponCacheRepository: CouponCacheRepositoryV9,
) {

    fun get(id: Long): CouponIssuePolicy = couponCacheRepository.getIssuePolicyOrLoad(id) {
        couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
            .let (CouponIssuePolicy::from)
    }
}