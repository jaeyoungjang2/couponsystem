package com.example.coupon.v8.infrastructure.cache

import com.example.coupon.common.CacheProperties
import com.example.coupon.common.CacheMetrics
import com.example.coupon.common.CouponIssuePolicy
import com.example.coupon.common.listLuaScript
import com.example.coupon.common.longLuaScript
import com.example.coupon.common.runForLong
import com.example.coupon.common.runForStrings
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.time.Duration

@Repository
class CouponCacheRepositoryV8(
    private val redis: StringRedisTemplate,
    private val mapper: ObjectMapper,
    private val properties: CacheProperties,
    private val cacheMetrics: CacheMetrics,
) {
    private val singleFlightScript = listLuaScript("lua/cache-single-flight.lua")
    private val releaseLockScript = longLuaScript("lua/release-lock.lua")

    fun getIssuePolicyOrLoad(id: Long, loader: () -> CouponIssuePolicy) =
        getOrLoad(
            cacheKey = "coupon:$id:issue-policy",
            lockKey = "coupon:$id:issue-policy:lock",
            loader = loader,
        )


    // cache aside
    private fun getOrLoad(
        cacheKey: String,
        lockKey: String,
        loader: () -> CouponIssuePolicy,
    ): CouponIssuePolicy {
        val token = UUID.randomUUID().toString()
        repeat(MAX_RETRIES) {
            val result = redis.runForStrings(
                singleFlightScript,
                listOf(cacheKey, lockKey),
                token, LOCK_TTL_MS,
            )

            when (result[0]) {
                "HIT" -> {
                    cacheMetrics.incrementCouponCacheHit()
                    return mapper.readValue(result[1], CouponIssuePolicy::class.java)
                }
                "LOAD" -> return try {
                    cacheMetrics.incrementCouponDbRead()
                    val response = loader()
                    val json = mapper.writeValueAsString(response)
                    redis.opsForValue().set(cacheKey, json, Duration.ofMillis(properties.ttlMs))
                    response
                } finally {
                    // redis.delete(lockKey) 로 하면 되지만
                    // lock을 잡은 thread가 lock을 해제할 수 있도록 해야함
                    redis.runForLong(
                        releaseLockScript,
                        listOf(lockKey),
                        token,
                    )
                }
                "WAIT" -> Thread.sleep(WAIT_BACKOFF_MS)
            }
        }
        throw IllegalStateException("쿠폰 캐시 채우기 timeout (key=$cacheKey)")
    }

    private companion object {
        const val MAX_RETRIES = 50
        const val WAIT_BACKOFF_MS = 20L
        const val LOCK_TTL_MS = 3_000L
    }
}