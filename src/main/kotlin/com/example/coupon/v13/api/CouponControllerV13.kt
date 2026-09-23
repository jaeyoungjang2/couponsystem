package com.example.coupon.v13.api

import com.apiece.coupon.api.dto.CouponResponse
import com.apiece.coupon.api.dto.CreateCouponRequest
import com.apiece.coupon.api.dto.IssuanceResponse
import com.apiece.coupon.application.CouponService
import com.apiece.coupon.application.WaitingRoom
import com.apiece.coupon.support.NoWaitingRoomPassException
import com.example.coupon.dto.CouponResponse
import com.example.coupon.dto.CreateCouponRequest
import com.example.coupon.dto.IssuanceResponse
import com.example.coupon.v13.application.WaitingRoom
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/coupons")
class CouponControllerV13(
    private val couponService: CouponService,
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