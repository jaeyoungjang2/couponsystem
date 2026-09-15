package com.example.coupon.v1.api

import com.example.coupon.dto.IssuanceResponse
import com.example.coupon.v1.application.IssuanceServiceV1
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/issuances")
class IssuanceControllerV1(
    private val issuanceServiceV1: IssuanceServiceV1
) {

    @PostMapping("/{issuanceId}/use")
    fun use(
        @PathVariable issuanceId: Long,
        @RequestHeader("X-User-Id") userId: Long,
        ): IssuanceResponse {
        val issuance = issuanceServiceV1.use(issuanceId, userId)
        return IssuanceResponse.from(issuance)
    }

}