package com.example.coupon.v7.application

import com.example.coupon.domain.CouponRepository
import com.example.coupon.support.CouponNotFoundException
import com.example.coupon.v7.infrastructure.cache.CouponCacheRepository
import org.springframework.stereotype.Service

@Service
class CouponIssuePolicyReader(
    private val couponRepository: CouponRepository,
    private val couponCacheRepository: CouponCacheRepository,
) {

    fun get(id: Long): CouponPolicy = couponCacheRepository.getIssuePolicyOrLoad(id) {
        couponRepository.findById(id)
            .orElseThrow { CouponNotFoundException() }
            .let (CouponPolicy::from)
    }
}