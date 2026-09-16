package com.example.coupon.v7.api

import com.example.coupon.v7.api.dto.CacheMetricsResponse
import com.example.coupon.v7.application.CacheMetrics
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/metrics/cache")
class CacheMetricController(
    private val cacheMetrics: CacheMetrics,
) {

    @GetMapping
    fun snapshot(): CacheMetricsResponse = CacheMetricsResponse.from(cacheMetrics.snapshot())

    @PostMapping("/reset")
    fun reset() {
        cacheMetrics.reset()
    }

}