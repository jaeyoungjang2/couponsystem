package com.example.coupon.v9.api

import com.example.coupon.dto.CouponResponse
import com.example.coupon.dto.CreateCouponRequest
import com.example.coupon.dto.IssuanceResponse
import com.example.coupon.v9.application.CouponServiceV9
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v9/coupons")
class CouponControllerV9(
    private val couponServiceV9: CouponServiceV9,
) {

    @PostMapping
    fun create(@RequestBody request: CreateCouponRequest): ResponseEntity<CouponResponse> {
        val coupon = couponServiceV9.createCoupon(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(coupon))
    }

    @PostMapping("/{couponId}/issue")
    fun issue(
        @PathVariable couponId: Long,
        @RequestHeader("X-User-Id") userId: Long,
    ): IssuanceResponse {
        val issuance = couponServiceV9.issue(couponId, userId)
        return IssuanceResponse.from(issuance)
    }
}