# 부하 테스트

## 사전 준비

- `docker compose up -d` 후 서비스가 8080 응답

```bash
git checkout <branch>
./gradlew --stop && ./gradlew jibDockerBuild && docker compose up -d
```

## v2

```bash
./scripts/load/v2/run.sh
```

`run.sh` 는 `reset → create_coupon → k6 → verify` 를 한 번에 실행한다.




## 결과 확인

█ TOTAL RESULTS

    HTTP
    http_req_duration..............: avg=17.28s min=0s     med=17.68s max=31.91s p(90)=29.56s p(95)=30.84s
      { expected_response:true }...: avg=16.73s min=478ms  med=16.27s max=31.76s p(90)=29.09s p(95)=30.37s
    http_req_failed................: 49.21% 4846 out of 9846
    http_reqs......................: 9846   265.370875/s

    EXECUTION
    dropped_iterations.............: 140157 3777.532572/s
    iteration_duration.............: avg=17.28s min=1.09ms med=17.68s max=31.91s p(90)=29.56s p(95)=30.84s
    iterations.....................: 9846   265.370875/s
    vus............................: 199    min=199          max=5000
    vus_max........................: 5000   min=2638         max=5000

    NETWORK
    data_received..................: 2.5 MB 66 kB/s
    data_sent......................: 1.3 MB 34 kB/s




running (0m37.1s), 0000/5000 VUs, 9846 complete and 0 interrupted iterations  
issue ✓ [=================================] 0000/5000 VUs  30s  5000.00 iters/s  
+-----------------+----------------+---------------+---------------+-------------+  
| issued_quantity | total_quantity | issuance_rows | over_issuance | count_match |  
+-----------------+----------------+---------------+---------------+-------------+  
|            5000 |           5000 |          5000 | OK            | OK          |  
+-----------------+----------------+---------------+---------------+-------------+  



비관적 락의 임계 범위는 줄였으나 따닥 문제 해결하지 못함 