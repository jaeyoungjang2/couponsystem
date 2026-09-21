package com.example.coupon.v12.infrastructure.cache

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "coupon.reconcile")
class ReconcileProperties(
    val intervalMs: Long,
    val gracePeriodMs: Long,
)