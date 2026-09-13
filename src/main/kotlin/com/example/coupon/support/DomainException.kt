package com.example.coupon.support

import org.springframework.http.HttpStatus


sealed class DomainException(
    val code: String,
    val httpStatus: HttpStatus,
    message: String,
) : RuntimeException(message)

class CouponNotFoundException(message: String = "쿠폰 행사를 찾을 수 없습니다") :
    DomainException("COUPON_NOT_FOUND", HttpStatus.NOT_FOUND, message)

class NotStartedException(message: String = "발급이 아직 시작되지 않았습니다") :
    DomainException("NOT_STARTED", HttpStatus.CONFLICT, message)

class SoldOutException(message: String = "쿠폰이 매진되었습니다") :
    DomainException("SOLD_OUT", HttpStatus.CONFLICT, message)

class AlreadyIssuedException(message: String = "이미 발급된 쿠폰입니다") :
    DomainException("ALREADY_ISSUED", HttpStatus.CONFLICT, message)