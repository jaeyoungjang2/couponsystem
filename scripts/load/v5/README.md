
# 부하 테스트

## v5

```bash
./scripts/load/v5/run.sh
```

`run.sh` 는 `reset → create_coupon → k6 → verify` 를 한 번에 실행한다.




## 결과 확인

    █ THRESHOLDS
  
      issue_latency
      ✗ 'p(99)<500' p(99)=4.6s
  
  
    █ TOTAL RESULTS
  
      CUSTOM
      issue_latency........: avg=503.25ms min=0s med=243.46ms max=22.16s p(90)=1.21s p(95)=1.89s
      status_other.........: 81663   2664.758031/s
  
      HTTP
      http_req_duration....: avg=503.25ms min=0s med=243.46ms max=22.16s p(90)=1.21s p(95)=1.89s
      http_req_failed......: 100.00% 81663 out of 81663
      http_reqs............: 81663   2664.758031/s
  
      EXECUTION
      dropped_iterations...: 68485   2234.744667/s
      iteration_duration...: avg=895.62ms min=0s med=471.65ms max=22.17s p(90)=2.33s p(95)=3.96s
      iterations...........: 81663   2664.758031/s
      vus..................: 3423    min=974            max=3739
      vus_max..............: 3929    min=2000           max=3929
  
      NETWORK
      data_received........: 17 MB   547 kB/s
      data_sent............: 8.4 MB  274 kB/s




running (0m30.6s), 0000/3930 VUs, 81663 complete and 0 interrupted iterations
burst ✓ [=================================] 0000/3930 VUs  30s  5000.00 iters/s
ERRO[0031] thresholds on metrics 'issue_latency' have been crossed
===== Worker 드레인 대기 (최대 60s, 3s 안정 시 확정) =====
드레인 완료. issuance rows = 0

+-----------------+----------------+---------------+---------------+-------------+
| issued_quantity | total_quantity | issuance_rows | over_issuance | count_match |
+-----------------+----------------+---------------+---------------+-------------+
|               0 |           5000 |             0 | OK            | OK          |
+-----------------+----------------+---------------+---------------+-------------+




비관적 락의 임계 범위는 줄였으나 따닥 문제 해결하지 못함 