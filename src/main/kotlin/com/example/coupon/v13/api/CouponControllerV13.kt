package com.example.coupon.v13.api

import com.example.coupon.dto.CouponResponse
import com.example.coupon.dto.CreateCouponRequest
import com.example.coupon.dto.IssuanceResponse
import com.example.coupon.support.NoWaitingRoomPassException
import com.example.coupon.v12.application.CouponServiceV12
import com.example.coupon.v13.application.WaitingRoom
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/coupons")
class CouponControllerV13(
    private val couponService: CouponServiceV12,
    private val waitingRoom: WaitingRoom,
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
        if (!waitingRoom.isAdmitted(couponId, userId)) throw NoWaitingRoomPassException()
        val issuance = couponService.issue(couponId, userId)
        return IssuanceResponse.from(issuance)
    }
}