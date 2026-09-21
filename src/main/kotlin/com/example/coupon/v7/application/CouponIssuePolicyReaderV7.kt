package com.example.coupon.v7.application

import com.example.coupon.domain.CouponRepository
import com.example.coupon.support.CouponNotFoundException
import com.example.coupon.common.CouponIssuePolicy
import com.example.coupon.v7.infrastructure.cache.CouponCacheRepositoryV7
import org.springframework.stereotype.Service

@Service
class CouponIssuePolicyReaderV7(
    private val couponRepository: CouponRepository,
    private val couponCacheRepository: CouponCacheRepositoryV7,
) {

    fun get(id: Long): CouponIssuePolicy = couponCacheRepository.getIssuePolicyOrLoad(id) {
        couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
            .let (CouponIssuePolicy::from)
    }
}