package com.example.coupon.v13.api

import com.apiece.coupon.api.dto.WaitingRoomResponse
import com.apiece.coupon.application.WaitingRoom
import com.example.coupon.v13.api.dto.WaitingRoomResponse
import com.example.coupon.v13.application.WaitingRoom
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/waiting-room")
class WaitingRoomControllerV13(
    private val waitingRoom: WaitingRoom,
) {

    @PostMapping("/{couponId}")
    fun enter(
        @PathVariable couponId: Long,
        @RequestHeader("X-User-Id") userId: Long,
    ): WaitingRoomResponse = WaitingRoomResponse.from(waitingRoom.enter(couponId, userId))

    @GetMapping("/{couponId}")
    fun status(
        @PathVariable couponId: Long,
        @RequestHeader("X-User-Id") userId: Long,
    ): WaitingRoomResponse =
        WaitingRoomResponse.from(waitingRoom.status(couponId, userId))
}