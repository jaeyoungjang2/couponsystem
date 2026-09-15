package com.example.coupon.v1.api

import com.example.coupon.dto.IssuanceResponse
import com.example.coupon.v1.application.IssuanceServiceV1
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/issuances")
class UserIssuanceControllerV1(
    private val issuanceServiceV1: IssuanceServiceV1
) {
    // 내 쿠폰 목록 조회
    @GetMapping
    fun listMine(@RequestHeader("X-User-Id") userId: Long): List<IssuanceResponse> {
        return issuanceServiceV1.findByUser(userId)
    }
}