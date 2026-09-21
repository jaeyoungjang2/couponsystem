package com.example.coupon.v11.application

import com.example.coupon.support.AlreadyIssuedException
import com.example.coupon.support.SoldOutException
import com.example.coupon.v11.infrastructure.cache.IssuanceRedisRepositoryV11
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class CouponIssuerV11(
    private val redisTemplate: StringRedisTemplate,
    private val issuanceRedisRepository: IssuanceRedisRepositoryV11,
) {


    fun tryIssue(couponId: Long, userId: Long) {
        when (issuanceRedisRepository.tryIssue(couponId, userId)) {
            1L -> Unit
            0L -> throw SoldOutException()
            -1L -> throw AlreadyIssuedException()
            else -> error("예상치 못한 Lua 결과")
        }
    }

    // 쿠폰 발행 -> 초기 갯수 저장
    fun initStock(couponId: Long, totalQuantity: Int) {
        redisTemplate.opsForValue().set(stockKey(couponId), totalQuantity.toString())
    }

    private fun stockKey(couponId: Long) = "coupon:$couponId:stock"
    private fun usersKey(couponId: Long) = "coupon:$couponId:users"
}