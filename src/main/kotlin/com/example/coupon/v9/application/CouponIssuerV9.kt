package com.example.coupon.v9.application

import com.example.coupon.support.AlreadyIssuedException
import com.example.coupon.support.SoldOutException
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component

@Component
class CouponIssuerV9(
    private val redisTemplate: StringRedisTemplate
) {

    private val script: RedisScript<Long> = RedisScript.of(
        ClassPathResource("lua/issueV4.lua"),
        Long::class.java,
    )

    fun tryIsusue(couponId: Long, userId: Long) {
        val raw = redisTemplate.execute(
            script,
            listOf(stockKey(couponId), usersKey(couponId)),
            userId.toString()
        ) ?: error("Lua 스크립트 결과가 null")

        when (raw) {
            1L -> Unit
            0L -> throw SoldOutException()
            -1L -> throw AlreadyIssuedException()
            else -> error("예상치 못한 Lua 결과: $raw")
        }
    }

    // 쿠폰 발행 -> 초기 갯수 저장
    fun initStock(couponId: Long, totalQuantity: Int) {
        redisTemplate.opsForValue().set(stockKey(couponId), totalQuantity.toString())
    }

    private fun stockKey(couponId: Long) = "coupon:$couponId:stock"
    private fun usersKey(couponId: Long) = "coupon:$couponId:users"
}