package com.example.coupon.v7.infrastructure.cache

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coupon.cache")
class CacheProperties(
    val ttlMs: Long,
    val simulatedLoadLatencyMs: Long,
) {
}