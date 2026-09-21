package com.example.coupon.v12.api

import com.example.coupon.dto.CouponResponse
import com.example.coupon.dto.CreateCouponRequest
import com.example.coupon.dto.IssuanceResponse
import com.example.coupon.v10.application.CouponServiceV10
import com.example.coupon.v12.application.CouponServiceV12
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v12/coupons")
class CouponControllerV12(
    private val couponService: CouponServiceV12,
) {

    @PostMapping
    fun create(@RequestBody request: CreateCouponRequest): ResponseEntity<CouponResponse> {
        val coupon = couponService.createCoupon(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(coupon))
    }

    @PostMapping("/{couponId}/issue")
    fun issue(
        @PathVariable couponId: Long,
        @RequestHeader("X-User-Id") userId: Long,
    ): IssuanceResponse {
        val issuance = couponService.issue(couponId, userId)
        return IssuanceResponse.from(issuance)
    }
}