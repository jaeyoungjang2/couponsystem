package com.example.coupon.v3.api

import com.example.coupon.dto.CouponResponse
import com.example.coupon.dto.CreateCouponRequest
import com.example.coupon.dto.IssuanceResponse
import com.example.coupon.v3.application.CouponServiceV3
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v3/coupons")
class CouponControllerV3(
    private val couponServiceV3: CouponServiceV3,
) {

    @PostMapping
    fun create(@RequestBody request: CreateCouponRequest): ResponseEntity<CouponResponse> {
        val coupon = couponServiceV3.createCoupon(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(coupon))
    }

    @PostMapping("/{couponId}/issue")
    fun issue(
        @PathVariable couponId: Long,
        @RequestHeader("X-User-Id") userId: Long,
    ): IssuanceResponse {
        val issuance = couponServiceV3.issue(couponId, userId)
        return IssuanceResponse.from(issuance)
    }
}