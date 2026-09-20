package com.example.coupon.v1.application

import com.example.coupon.dto.IssuanceResponse
import com.example.coupon.domain.CouponRepository
import com.example.coupon.domain.Issuance
import com.example.coupon.domain.IssuanceRepository
import com.example.coupon.domain.IssuanceStatus
import com.example.coupon.support.AlreadyUsedException
import com.example.coupon.support.ExpiredException
import com.example.coupon.support.IssuanceNotFoundException
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class IssuanceServiceV1(
    private val issuanceRepository: IssuanceRepository,
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun use(issuanceId: Long, userId: Long): Issuance {
        val issuance = issuanceRepository.findById(issuanceId)
            .orElseThrow { IssuanceNotFoundException() };

        // 사용 가능한 쿠폰인지 확인
        // when 절은 타입에 대한 것도, 값에 대한 것도 검증이 가능하다.
        when (issuance.status) {
            IssuanceStatus.USED -> throw AlreadyUsedException()
            IssuanceStatus.EXPIRED -> throw ExpiredException()
            IssuanceStatus.ISSUED -> Unit
        }

        val now = LocalDateTime.now()
        if (issuance.isExpired(now)) {
            throw ExpiredException()
        }

        // 쿠폰 사용 가능하다면 쿠폰 갯수 수정
        issuance.markUsed(now)
        return issuance
    }

    fun findByUser(userId: Long): List<IssuanceResponse> {
        return issuanceRepository.findByUserIdOrderByIssuedAtDesc(userId)
            .map(IssuanceResponse::from)
    }
}
