package com.example.coupon.v11.api

import com.example.coupon.v11.api.dto.IssuanceDltReplayResponse
import com.example.coupon.v11.api.dto.IssuanceDltResponse
import com.example.coupon.v11.api.dto.ReplayIssuanceDltRequest
import com.example.coupon.v11.application.IssuanceDltReplayService
import com.example.coupon.v11.application.IssuanceDltService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/issuance/dlt")
class AdminIssuanceDltController(
    private val issuanceDltService: IssuanceDltService,
    private val issuanceDltReplayService: IssuanceDltReplayService,
) {
    @GetMapping
    fun findRecent(): List<IssuanceDltResponse> =
        issuanceDltService.findRecent().map(IssuanceDltResponse::from)

    @PostMapping("/replay")
    fun replay(@RequestBody request: ReplayIssuanceDltRequest): IssuanceDltReplayResponse =
        IssuanceDltReplayResponse(issuanceDltReplayService.replay(request.ids))
}

