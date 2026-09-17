package com.example.coupon.common

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coupon.cache")
class CacheProperties(
    val ttlMs: Long,
    val freshMs: Long,
    val simulatedLoadLatencyMs: Long,
) {
}