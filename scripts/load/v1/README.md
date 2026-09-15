# 부하 테스트

## 사전 준비

- macOS: `brew install k6 jq`
- Windows: Git Bash 에서 실행한다. `winget install jqlang.jq`, `winget install k6 --source winget`
- `docker compose up -d` 후 서비스가 8080 응답

```bash
git checkout <branch>
./gradlew --stop && ./gradlew jibDockerBuild && docker compose up -d
```

## v1

```bash
./scripts/load/v1/run.sh
```

`run.sh` 는 `reset → create_coupon → k6 → verify` 를 한 번에 실행한다.




## 결과 확인

█ TOTAL RESULTS

    HTTP
    http_req_duration..............: avg=14.83s min=0s     med=18.56s max=20.04s p(90)=19.32s p(95)=19.47s
      { expected_response:true }...: avg=15.44s min=65.1ms med=18.59s max=20.04s p(90)=19.32s p(95)=19.47s
    http_req_failed................: 3.94%  513 out of 12989
    http_reqs......................: 12989  261.151509/s

    EXECUTION
    dropped_iterations.............: 137010 2754.666893/s
    iteration_duration.............: avg=14.84s min=0s     med=18.56s max=20.04s p(90)=19.32s p(95)=19.47s
    iterations.....................: 12989  261.151509/s
    vus............................: 165    min=165          max=5000
    vus_max........................: 5000   min=2560         max=5000

    NETWORK
    data_received..................: 3.3 MB 67 kB/s
    data_sent......................: 1.7 MB 34 kB/s


running (0m49.7s), 0000/5000 VUs, 12989 complete and 0 interrupted iterations  
issue ✓ [=================================] 0000/5000 VUs  30s  5000.00 iters/s


+-----------------+----------------+---------------+---------------+-------------+  
| issued_quantity | total_quantity | issuance_rows | over_issuance | count_match |  
+-----------------+----------------+---------------+---------------+-------------+  
|            1289 |           5000 |         12476 | FAIL          | FAIL        |  
+-----------------+----------------+---------------+---------------+-------------+  
FAIL: 초과발급 (실제발급 12476 > 재고 5000)


## 결과 정리

### 1. HTTP 결과 해석

계획한 요청  
```
초당 5,000건 × 30초 = 150,000건
```

실제 결과
- 요청 응답시간이 너무 길어 VU가 계속 점유되었고, maxVUs: 5000까지 모두 사용한 뒤 추가 요청을 생성할 수 없었다
```
완료 요청:       12,989건
Dropped:        137,010건 -> 서버가 오류를 반환한 요청이 아니라, k6가 목표 시간에 요청 자체를 시작하지 못한 횟수
합계:           149,999건
```

### 2. 느린 응답 시간
```
평균: 14.83초
중앙값: 18.56초
p90: 19.32초
p95: 19.47초
최대: 20.04초
```

확인 해 볼 사항
- DB 커넥션 풀 대기
- 행 락 대기
- 서버 요청 타임아웃
- 커넥션 획득 타임아웃
- 애플리케이션 스레드 풀 고갈
- MySQL 락 대기 또는 트랜잭션 적체


### 1. lost update 발생
- 동시에 같은 카운터를 읽고 덮어쓰는 상황  
- issued_quantity가 1,289인데 발급 내역은 12,476건  

예시
```
요청 A: issued_quantity = 100 읽음
요청 B: issued_quantity = 100 읽음

요청 A: 101로 저장
요청 B: 101로 저장
```

