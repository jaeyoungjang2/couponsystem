package com.example.coupon.v11.application

import com.example.coupon.common.CacheMetrics
import com.example.coupon.v11.infrastructure.cache.SoldOutProperties
import com.example.coupon.v11.infrastructure.cache.SoldOutRedisRepositoryV10
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class SoldOutState(
    private val soldOutRedisRepository: SoldOutRedisRepositoryV10,
    private val cacheMetrics: CacheMetrics,
    properties: SoldOutProperties,
) {

    private val cache: LoadingCache<Long, Boolean> = Caffeine.newBuilder()
        // 캐시에 값을 저장한 후 설정된 시간이 지나면 만료
        .expireAfterWrite(Duration.ofMillis(properties.fastPathTtlMs))
        // 로컬 캐시에 저장할 수 있는 쿠폰 개수를 제한
        // 한도를 넘으면 오래되거나 사용 빈도가 낮은 항목부터 제거됨
        .maximumSize(MAX_TRACKED_COUPONS)
        // 캐시에 해당 couponId가 없을 때 실행되는 로딩 함수.
        .build { couponId ->
            cacheMetrics.incrementSoldOutRedisExists()
            soldOutRedisRepository.isFlagged(couponId)
        }

    fun isSoldOut(couponId: Long): Boolean {
        // caffein cache에서 매진 확인
        val soldOut = cache.get(couponId) ?: false
        if (soldOut) cacheMetrics.incrementSoldOutFastPathHit()
        return soldOut
    }

    private companion object {
        const val MAX_TRACKED_COUPONS = 1_000L
    }
}