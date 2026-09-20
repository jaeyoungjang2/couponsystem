package com.example.coupon.v11.application

import com.example.coupon.common.CouponIssuePolicy
import com.example.coupon.domain.CouponRepository
import com.example.coupon.support.CouponNotFoundException
import com.example.coupon.v11.infrastructure.cache.CouponCacheRepositoryV10
import org.springframework.stereotype.Service

@Service
class CouponIssuePolicyReaderV11(
    private val couponRepository: CouponRepository,
    private val couponCacheRepository: CouponCacheRepositoryV10,
) {

    fun get(id: Long): CouponIssuePolicy = couponCacheRepository.getIssuePolicyOrLoad(id) {
        couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
            .let (CouponIssuePolicy::from)
    }
}