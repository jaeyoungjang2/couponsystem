# 부하 테스트

## 사전 준비

- macOS: `brew install k6 jq`
- Windows: Git Bash 에서 실행한다. `winget install jqlang.jq`, `winget install k6 --source winget`
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
    http_req_duration..............: avg=28.16s min=0s       med=30.54s max=48.84s p(90)=45.57s p(95)=47.1s
      { expected_response:true }...: avg=26.09s min=411.83ms med=26.66s max=48.37s p(90)=44.3s  p(95)=46.35s
    http_req_failed................: 39.62% 3282 out of 8282
    http_reqs......................: 8282   151.220934/s

    EXECUTION
    dropped_iterations.............: 141721 2587.681963/s
    iteration_duration.............: avg=28.16s min=0s       med=30.54s max=48.84s p(90)=45.57s p(95)=47.1s
    iterations.....................: 8282   151.220934/s
    vus............................: 771    min=771          max=5000
    vus_max........................: 5000   min=2653         max=5000

    NETWORK
    data_received..................: 2.1 MB 38 kB/s
    data_sent......................: 1.0 MB 19 kB/s




running (0m54.8s), 0000/5000 VUs, 8282 complete and 0 interrupted iterations
issue ✓ [=================================] 0000/5000 VUs  30s  5000.00 iters/s
+-----------------+----------------+---------------+---------------+-------------+
| issued_quantity | total_quantity | issuance_rows | over_issuance | count_match |
+-----------------+----------------+---------------+---------------+-------------+
|            5000 |           5000 |          5000 | OK            | OK          |
+-----------------+----------------+---------------+---------------+-------------+



## 결과 정리
정합성은 지켰지만 성능은 좋지 않은 상태.

### 1. HTTP 결과 해석

평균 응답 시간 28초이고 
p95가 47초이며, 
목표로 한 요청 중 약 14만 건은 실행조차 되지 못했습니다.
