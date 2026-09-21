package com.example.coupon.v10.infrastructure.cache

import com.example.coupon.common.longLuaScript
import com.example.coupon.common.runForLong
import com.example.coupon.infrastructure.cache.SoldOutProperties
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class IssuanceRedisRepositoryV10(
    private val redis: StringRedisTemplate,
    private val soldOutProperties: SoldOutProperties,
) {

    private val issueScript = longLuaScript("lua/issueV10.lua")

    fun tryIssue(couponId: Long, userId: Long): Long =

        // 반환: 1=성공, 0=매진, -1=중복 발급
        redis.runForLong(
            issueScript,
            listOf("coupon:$couponId:stock", "coupon:$couponId:users", "coupon:$couponId:sold_out"),
            userId, soldOutProperties.ttlSeconds,
        )
}