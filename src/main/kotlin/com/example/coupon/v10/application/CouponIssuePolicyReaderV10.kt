package com.example.coupon.v10.application

import com.example.coupon.common.CouponIssuePolicy
import com.example.coupon.domain.CouponRepository
import com.example.coupon.support.CouponNotFoundException
import com.example.coupon.v10.infrastructure.cache.CouponCacheRepositoryV10
import com.example.coupon.v9.infrastructure.cache.CouponCacheRepositoryV9
import org.springframework.stereotype.Service

@Service
class CouponIssuePolicyReaderV10(
    private val couponRepository: CouponRepository,
    private val couponCacheRepository: CouponCacheRepositoryV10,
) {

    fun get(id: Long): CouponIssuePolicy = couponCacheRepository.getIssuePolicyOrLoad(id) {
        couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
            .let (CouponIssuePolicy::from)
    }
}