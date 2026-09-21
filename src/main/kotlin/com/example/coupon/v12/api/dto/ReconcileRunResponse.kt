package com.example.coupon.v12.api.dto

import com.example.coupon.v12.batch.ReconcileReport

class ReconcileRunResponse(
    val autoFixed: Int,
    val driftAlerts: Int,
    val redisDbDrift: Long,
) {
    companion object {
        fun from(report: ReconcileReport): ReconcileRunResponse = ReconcileRunResponse(
            autoFixed = report.autoFixed,
            driftAlerts = report.driftAlerts,
            redisDbDrift = report.redisDbDrift,
        )
    }
}