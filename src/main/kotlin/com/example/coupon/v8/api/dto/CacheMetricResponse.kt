package com.example.coupon.v8.api.dto

import com.example.coupon.common.CacheMetricsSnapshot

class CacheMetricsResponse(
    val couponDbReads: Long,
    val couponCacheHits: Long,
    val soldOutRedisExists: Long,
    // L1 캐시를 사용했는지
    val soldOutFastPathHits: Long,
) {
    companion object {
        fun from(snapshot: CacheMetricsSnapshot): CacheMetricsResponse = CacheMetricsResponse(
            couponDbReads = snapshot.couponDbReads,
            couponCacheHits = snapshot.couponCacheHits,
            soldOutRedisExists = snapshot.soldOutRedisExists,
            soldOutFastPathHits = snapshot.soldOutFastPathHits,
        )
    }
}