package com.example.coupon.v12.api

import com.example.coupon.v12.api.dto.ReconcileRunResponse
import com.example.coupon.v12.batch.Reconciler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/reconcile")
class AdminReconcileController(
    private val reconciler: Reconciler,
) {
    @PostMapping("/run")
    fun run(): ReconcileRunResponse =
        ReconcileRunResponse.from(reconciler.auditAll())
}