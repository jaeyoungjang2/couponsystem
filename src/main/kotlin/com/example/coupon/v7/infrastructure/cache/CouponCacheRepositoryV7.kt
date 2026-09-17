package com.example.coupon.v7.infrastructure.cache

import com.example.coupon.common.CacheProperties
import com.example.coupon.common.CacheMetrics
import com.example.coupon.common.CouponIssuePolicy
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Repository
class CouponCacheRepositoryV7(
    private val redis: StringRedisTemplate,
    private val mapper: ObjectMapper,
    private val properties: CacheProperties,
    private val cacheMetrics: CacheMetrics,
) {

    fun getIssuePolicyOrLoad(id: Long, loader: () -> CouponIssuePolicy) =
        getOrLoad(
            key = "coupon:$id:issue-policy",
            loader = loader,
        )


    // cache aside
    private fun getOrLoad(
        key: String,
        loader: () -> CouponIssuePolicy,
    ): CouponIssuePolicy {
        redis.opsForValue().get(key)?.let { cached ->
            cacheMetrics.incrementCouponCacheHit()
            return mapper.readValue(cached, CouponIssuePolicy::class.java)
        }
        cacheMetrics.incrementCouponDbRead()
        val response = loader()
        redis.opsForValue().set(key, mapper.writeValueAsString(response), Duration.ofMillis(properties.ttlMs))
        return response
    }
}