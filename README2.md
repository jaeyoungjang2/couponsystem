
## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Kotlin 2.3.21, Java 25 |
| Framework | Spring Boot 4.1.1 |
| Database | MySQL 8.4, Spring Data JPA |
| Cache | Redis 8.0, Caffeine |
| Messaging | Apache Kafka 3.8.0 |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus, Grafana |
| Load Test | k6 |
| Infrastructure | Docker Compose |

## 버전별 변경점

| 버전 | 핵심 변경 | 해결한 문제 | 남은 한계 |
|---|---|---|---|
| v1 | JPA 기반 동기 발급 | 기본 쿠폰 발급 기능 구현 | Lost Update로 초과 발급 가능 |
| v2 | 비관적 락 적용 | DB 발급 수량 정합성 보장 | 락 대기로 응답시간과 처리량 악화 |
| v3 | Redis Lua 재고 차감 | 재고 확인과 차감 원자화 | 중복 발급 확인과 DB 저장이 동기 처리 |
| v4 | Redis 중복 방지와 인메모리 큐 | 요청 처리와 DB 저장 분리 | 큐 포화 및 프로세스 종료 시 이벤트 유실 |
| v5 | Spring 비동기 이벤트 | 비동기 처리 구조 단순화 | 메모리 큐이므로 내구성 미보장 |
| v6 | Kafka 기반 비동기 처리 | 이벤트 내구성, 재시도 및 DLT 확보 | Redis와 DB 사이의 분산 정합성 문제 |
| v7 | Redis Cache Aside | 발급 정책 조회의 DB 부하 감소 | 캐시 만료 시 Cache Stampede 가능 |
| v8 | Single Flight | 동일 키에 대한 동시 DB 조회 억제 | 대기 요청과 분산 락 관리 필요 |
| v9 | Stale While Revalidate | 캐시 갱신 중에도 빠른 응답 제공 | 짧은 시간 동안 오래된 값 반환 가능 |
| v10 | Redis 매진 플래그와 Caffeine L1 | 매진 이후 불필요한 Redis 처리 감소 | L1 캐시 TTL 동안 상태 반영 지연 가능 |
| v11 | DLT 로그 저장 및 Replay | 실패 이벤트의 조회와 운영 복구 | Redis-DB 불일치를 자동 탐지하지 못함 |
| v12 | 정합성 대사와 안전한 자동 보정 | Redis 누락과 매진 플래그 불일치 복구 | 위험한 DB 불일치는 경고만 제공 |

## 버전별 상세 설명

### v1 — DB 기반 동기 발급

쿠폰 조회, 발급 가능 여부 확인, 발급 수량 증가 및 발급 내역 저장을 하나의 DB 트랜잭션에서 처리합니다.

여러 요청이 동시에 같은 `issuedQuantity`를 읽고 갱신하면 Lost Update가 발생할 수 있습니다. 이 경우 DB의 발급 수량과 실제 발급 내역이 일치하지 않거나, 쿠폰 재고보다 많은 발급 내역이 생성될 수 있습니다.

주요 특징:

- 기본 쿠폰 생성 및 발급 기능
- JPA 기반 동기 처리
- 쿠폰 발급 기간과 매진 여부 검사
- DB 조회를 이용한 사용자 중복 발급 검사
- 동시 요청 시 Lost Update 및 초과 발급 가능

부하 테스트:

```bash
./scripts/load/v1/run.sh
```

### v2 — 비관적 락을 이용한 정합성 보장

쿠폰을 조회할 때 `PESSIMISTIC_WRITE` 락을 획득하여 같은 쿠폰에 대한 발급 요청을 직렬화합니다.

초과 발급은 방지할 수 있지만 모든 요청이 하나의 DB 행 락을 기다리게 됩니다. 따라서 트래픽이 증가하면 DB 커넥션과 애플리케이션 스레드가 락 대기 상태로 오래 유지되어 응답시간이 증가하고 처리량이 감소합니다.

주요 특징:

- 쿠폰 조회 시 비관적 락 적용
- Lost Update 및 초과 발급 방지
- DB 정합성 보장
- 높은 트래픽에서 락 경합과 커넥션 풀 대기 증가

부하 테스트:

```bash
./scripts/load/v2/run.sh
```

### v3 — Redis Lua 기반 재고 차감

재고 확인과 차감을 Redis Lua Script에서 하나의 원자적 연산으로 실행합니다. DB 비관적 락을 제거하고 Redis에서 빠르게 재고를 선점하여 DB 락의 임계 구역을 줄입니다.

다만 Redis에서는 재고만 관리하고 사용자 중복 발급 여부는 DB에서 별도로 확인합니다. 따라서 같은 사용자가 매우 짧은 간격으로 여러 요청을 보내는 경우 중복 발급 경쟁 조건이 남아 있습니다.

주요 특징:

- 쿠폰 생성 시 Redis 재고 초기화
- Lua Script를 이용한 재고 확인과 차감 원자화
- DB 비관적 락 제거
- 중복 발급 확인은 여전히 DB에 의존

부하 테스트:

```bash
./scripts/load/v3/run.sh
```

### v4 — Redis 중복 방지와 인메모리 큐

Redis Lua Script에서 재고 차감과 발급 사용자 등록을 함께 처리합니다. DB 발급 내역 저장은 `LinkedBlockingQueue`와 별도 Worker를 이용하여 비동기로 처리합니다.

API 응답과 DB 저장이 분리되어 응답시간은 개선되지만, Redis 처리가 성공한 뒤 큐 등록이 실패하면 Redis와 DB 사이에 불일치가 발생합니다. 애플리케이션이 종료되면 메모리에 남아 있던 이벤트도 유실됩니다.

주요 특징:

- Redis Set을 이용한 사용자 중복 발급 방지
- 재고 차감과 사용자 등록을 Lua Script로 원자 처리
- 인메모리 큐와 전용 Worker를 이용한 비동기 DB 저장
- DB 발급 내역 저장과 발급 수량 증가를 하나의 트랜잭션으로 처리
- 큐 용량 초과 시 `QueueFullException`
- 프로세스 종료 시 처리되지 않은 이벤트 유실 가능

부하 테스트:

```bash
./scripts/load/v4/run.sh
```

### v5 — Spring 비동기 이벤트 처리

직접 구현한 Worker 대신 Spring `ApplicationEventPublisher`, `@EventListener`, `@Async` 및 `ThreadPoolTaskExecutor`를 사용합니다.

비동기 처리 구조는 단순해졌지만 이벤트가 애플리케이션 메모리에만 존재한다는 점은 v4와 같습니다. Executor 큐가 가득 차면 작업이 거절될 수 있으며, 애플리케이션 장애 시 처리되지 않은 이벤트가 유실됩니다.

주요 특징:

- Spring Event 기반 발급 이벤트 발행
- `@Async` 기반 비동기 DB 저장
- 단일 Worker와 크기가 제한된 Task Queue 사용
- Task 거절 및 애플리케이션 장애 시 이벤트 유실 가능
- Redis 변경과 DB 트랜잭션을 함께 롤백할 수 없음

부하 테스트:

```bash
./scripts/load/v5/run.sh
```

### v6 — Kafka 기반 비동기 발급

발급 요청 이벤트를 Kafka에 저장하고 Consumer가 DB 발급 내역과 쿠폰 발급 수량을 반영합니다.

인메모리 큐와 달리 애플리케이션이 재시작되어도 Kafka에 저장된 이벤트를 다시 소비할 수 있습니다. Consumer 처리 실패 시 재시도하고, 최종 실패한 메시지는 DLT로 전달합니다.

주요 특징:

- Kafka Producer와 Consumer 구성
- 발급 이벤트의 내구성 확보
- `issuance.requested` 토픽을 3개 Partition으로 구성
- Consumer concurrency 3
- Consumer 실패 시 1초 간격으로 3회 재시도
- 최종 실패 메시지를 `issuance.requested.DLT`로 전달
- DB Unique 제약과 적용 여부 확인을 이용한 멱등 처리

부하 테스트:

```bash
./scripts/load/v6/run.sh
```

### v7 — 발급 정책 Cache Aside

발급 시작 시각과 유효기간 같은 쿠폰 발급 정책을 Redis에 캐싱합니다. 캐시에 데이터가 없으면 DB에서 조회한 뒤 Redis에 저장합니다.

반복적인 DB 조회는 감소하지만 캐시가 만료되는 순간 여러 요청이 동시에 DB를 조회하는 Cache Stampede가 발생할 수 있습니다.

주요 특징:

- Redis Cache Aside 적용
- 쿠폰 발급 정책 DB 조회 감소
- 캐시 Hit와 DB 조회 횟수를 확인하는 Micrometer 지표 추가
- 캐시 만료 시 Cache Stampede 발생 가능

부하 테스트:

```bash
./scripts/load/v7/run.sh policy
```

### v8 — Single Flight로 Cache Stampede 방지

캐시가 만료되었을 때 하나의 요청만 분산 락을 획득하여 DB를 조회하고, 나머지 요청은 캐시가 채워질 때까지 대기합니다.

락을 획득한 요청만 락을 해제할 수 있도록 UUID 토큰과 Lua Script를 사용합니다. 단순히 `DELETE lockKey`를 실행하면 다른 요청이 새로 획득한 락까지 제거할 수 있으므로 토큰을 비교한 후 해제합니다.

주요 특징:

- Redis 기반 Single Flight 적용
- 동일 쿠폰에 대한 중복 DB 조회 억제
- UUID 토큰을 이용한 락 소유권 검증
- 최대 재시도 횟수와 대기 간격 설정
- 락 보유 요청이 느려지면 다른 요청의 대기시간 증가

부하 테스트:

```bash
./scripts/load/v8/run.sh policy
```

### v9 — Stale While Revalidate

캐시가 Fresh 구간을 지났더라도 Redis TTL이 만료되지 않았다면 기존 값을 즉시 반환합니다. 동시에 락을 획득한 하나의 요청이 백그라운드에서 데이터를 갱신합니다.

사용자 요청이 캐시 갱신 완료를 기다리지 않기 때문에 캐시 만료 구간에서도 응답시간을 안정적으로 유지할 수 있습니다.

주요 특징:

- Fresh 구간과 Stale 구간 분리
- Stale 데이터 즉시 반환
- 별도 Executor에서 백그라운드 갱신
- 백그라운드 갱신에도 Single Flight 적용
- 발급 요청 전체를 감싸던 불필요한 DB 트랜잭션 제거
- 짧은 시간 동안 오래된 쿠폰 정책이 반환될 수 있음

부하 테스트:

```bash
./scripts/load/v9/run.sh policy
```

### v10 — 매진 Fast Path

마지막 재고가 차감될 때 Lua Script가 Redis에 매진 플래그를 함께 기록합니다. 애플리케이션은 Caffeine L1 캐시로 매진 여부를 확인하여 매진된 쿠폰의 요청을 발급 Lua Script 실행 전에 빠르게 차단합니다.

주요 특징:

- Redis 매진 플래그 추가
- 마지막 재고 차감과 매진 플래그 설정 원자화
- Caffeine `LoadingCache` 기반 애플리케이션 L1 캐시
- 매진 이후 Redis 호출과 발급 Lua Script 실행 감소
- L1 캐시 TTL 동안 매진 상태 반영이 지연될 수 있음

부하 테스트:

```bash
./scripts/load/v10/run.sh sellout
```

### v11 — Kafka DLT 운영 복구

Kafka DLT 메시지를 단순 보관하는 데서 그치지 않고 DB에 기록하여 관리자가 실패 원인과 처리 상태를 확인할 수 있도록 합니다.

관리자는 `PENDING` 상태의 DLT 메시지를 선택하여 원래 발급 토픽으로 다시 발행할 수 있습니다. DB Writer는 같은 사용자와 쿠폰의 발급 내역이 이미 존재하는지 확인하여 Replay에 의한 중복 반영을 방지합니다.

주요 특징:

- DLT 메시지를 `issuance_dlt_log` 테이블에 저장
- Topic, Partition, Offset 조합을 이용한 중복 DLT 기록 방지
- 최근 DLT 로그 조회 API
- 선택한 DLT 메시지 Replay API
- `PENDING`, `REPLAYED` 상태 관리
- DB Writer의 멱등성 강화

관리 API:

```text
GET  /admin/issuance/dlt
POST /admin/issuance/dlt/replay
```

### v12 — Redis와 DB 정합성 대사

Redis 재고, 발급자 명단, 매진 플래그와 DB 발급 데이터를 주기적으로 비교합니다. 발급이 발생한 쿠폰은 Redis Sorted Set에 기록하여 최근 변경된 쿠폰만 선별적으로 점검합니다.

안전하게 판단할 수 있는 불일치만 자동 보정하고, 원인을 단정하기 어려운 DB 측 불일치는 데이터를 임의로 변경하지 않고 경고 로그와 대사 결과로 남깁니다.

주요 특징:

- 최근 발급 쿠폰을 Redis Sorted Set에 기록
- 스케줄 기반 정합성 점검
- 관리자 수동 대사 API 제공
- 잘못된 매진 플래그 자동 설정 및 해제
- DB에는 존재하지만 Redis 사용자 Set에 누락된 사용자 자동 복구
- 재고 음수 및 DB 발급 수 불일치는 경고만 기록
- Grace Period를 적용하여 아직 Kafka 처리 중인 이벤트를 대사 대상에서 제외

관리 API:

```text
POST /admin/reconcile/run
```

대사 테스트:

```bash
./scripts/load/v12/run.sh
```

## 시스템 실행

### 사전 준비

- Java 25
- Docker 및 Docker Compose
- k6
- jq

macOS:

```bash
brew install k6 jq
```

Windows에서는 Git Bash를 기준으로 실행합니다.

```bash
winget install k6 --source winget
winget install jqlang.jq
```

### 인프라 실행

```bash
docker compose up -d mysql redis kafka prometheus grafana
```

실행 상태 확인:

```bash
docker compose ps
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 접속 정보:

| 서비스 | 주소 |
|---|---|
| Coupon API | `http://localhost:8080` |
| MySQL | `localhost:3306` |
| Redis | `localhost:6379` |
| Kafka | `localhost:9094` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |

Grafana 기본 계정은 `admin / admin`입니다.

## API 사용 예시

각 버전의 API는 `/api/v{version}/coupons` 형식으로 구분됩니다.

### 쿠폰 생성

```bash
curl -X POST http://localhost:8080/api/v12/coupons \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "선착순 할인 쿠폰",
    "totalQuantity": 1000,
    "validityDays": 7
  }'
```

### 쿠폰 발급

```bash
curl -X POST http://localhost:8080/api/v12/coupons/1/issue \
  -H 'X-User-Id: 1001'
```

### DLT 로그 조회

```bash
curl http://localhost:8080/admin/issuance/dlt
```

### DLT 메시지 재처리

```bash
curl -X POST http://localhost:8080/admin/issuance/dlt/replay \
  -H 'Content-Type: application/json' \
  -d '{"ids":[1]}'
```

### 전체 쿠폰 정합성 점검

```bash
curl -X POST http://localhost:8080/admin/reconcile/run
```

## 부하 테스트

버전별 테스트 스크립트는 `scripts/load/v{version}` 경로에 있습니다.

일반적인 테스트 흐름은 다음과 같습니다.

```text
데이터 초기화 → 테스트 쿠폰 생성 → k6 실행 → DB 및 Redis 결과 검증
```

실행 예시:

```bash
./scripts/load/v1/run.sh
./scripts/load/v2/run.sh
./scripts/load/v3/run.sh
./scripts/load/v4/run.sh
./scripts/load/v5/run.sh
./scripts/load/v6/run.sh

./scripts/load/v7/run.sh policy
./scripts/load/v8/run.sh policy
./scripts/load/v9/run.sh policy
./scripts/load/v10/run.sh sellout

./scripts/load/v12/run.sh
```

테스트 결과를 비교할 때 다음 지표를 함께 확인합니다.

- `http_req_duration`: 전체 요청 응답시간
- `p(95)`, `p(99)`: 상위 지연 요청의 응답시간
- `http_req_failed`: HTTP 실패율
- `dropped_iterations`: 목표 요청률을 생성하지 못한 횟수
- DB 발급 내역 수
- 쿠폰의 `issuedQuantity`
- Redis 잔여 재고
- Redis 발급 사용자 수
- Kafka Consumer Lag 및 DLT 메시지 수

`dropped_iterations`는 서버가 오류를 반환한 요청 수가 아니라, k6가 설정한 요청률을 맞추기 위해 필요한 VU를 확보하지 못해 요청 자체를 시작하지 못한 횟수입니다.

## 모니터링

애플리케이션은 Spring Boot Actuator와 Micrometer를 통해 Prometheus 지표를 제공합니다.

주요 확인 대상:

- HTTP 요청 처리량과 응답시간
- JVM Thread 상태
- HikariCP Active, Idle, Pending Connection
- Redis 캐시 Hit 및 DB 조회 횟수
- 매진 Fast Path Hit 횟수
- Kafka Consumer Lag
- DLT 메시지 발생 여부
- Redis-DB 정합성 대사 결과

Prometheus 설정은 `monitoring/prometheus/prometheus.yml`에서 확인할 수 있습니다.

## 정합성 및 장애 복구 전략

이 프로젝트는 Redis를 실시간 발급 판단의 기준으로 사용하고 MySQL을 최종 발급 내역 저장소로 사용합니다.

Redis 재고 차감과 Kafka 메시지 발행은 하나의 분산 트랜잭션으로 묶여 있지 않으므로 다음과 같은 불일치가 발생할 수 있습니다.

1. Redis 발급 성공 후 Kafka 이벤트 발행 실패
2. Kafka 이벤트 발행 성공 후 DB 저장 실패
3. DB 저장은 성공했지만 Redis 사용자 명단 누락
4. Redis 매진 플래그와 실제 재고 불일치

복구 방식은 다음과 같습니다.

| 상황 | 처리 방식 |
|---|---|
| Kafka Consumer 처리 실패 | 재시도 후 DLT 전송 |
| DLT에 저장된 발급 이벤트 | 관리자 확인 후 Replay |
| Redis 사용자 명단 누락 | DB 발급 내역을 기준으로 자동 복구 |
| 잘못된 매진 플래그 | Redis 재고를 기준으로 자동 설정 또는 해제 |
| Redis 재고 음수 | 자동 수정하지 않고 경고 기록 |
| DB 발급 수와 Redis 재고 불일치 | 자동 수정하지 않고 경고 및 대사 결과 반환 |

자동 보정은 시스템이 올바른 값을 명확하게 판단할 수 있는 경우에만 수행합니다. 원인을 단정하기 어려운 데이터는 임의로 수정하지 않고 운영자가 확인할 수 있도록 남깁니다.

## 프로젝트 구조

```text
src/main/kotlin/com/example/coupon
├── common                 # 공통 캐시 설정과 지표
├── domain                 # Coupon, Issuance 도메인
├── infrastructure/cache   # 공통 매진 상태 및 L1 캐시
├── support                # 예외 및 공통 예외 처리
├── v1 ~ v6                # 동시성 제어와 비동기 처리 발전 과정
├── v7 ~ v10               # 캐시 및 매진 Fast Path 최적화
├── v11                    # Kafka DLT 저장과 Replay
└── v12                    # Redis-DB 정합성 대사

src/main/resources
├── application.yaml
└── lua                    # Redis 원자 처리 및 캐시 제어 Lua Script

scripts/load
└── v1 ~ v12               # 버전별 부하 및 장애 상황 재현 스크립트
```

## 현재 구조의 한계

- Redis 재고 차감과 Kafka 메시지 발행은 원자적으로 처리되지 않습니다.
- Kafka Producer의 `acks=1` 설정은 리더 Broker 장애 시 메시지 유실 가능성이 있습니다.
- Redis 장애 시 발급 요청을 처리할 대체 경로가 없습니다.
- DLT Replay 및 정합성 보정 API에는 별도의 인증과 권한 제어가 필요합니다.
- v12의 경고는 로그와 API 응답으로 제공되며 외부 알림 채널과 연결되어 있지 않습니다.
- 운영 환경에서는 Kafka Broker 다중화, Redis 고가용성, 모니터링 알림 및 관리자 작업 감사 로그가 필요합니다.

## 핵심 학습 내용

- DB 락만으로 정합성을 지키면 성능 병목이 발생할 수 있습니다.
- Redis Lua Script를 사용하면 여러 Redis 명령을 원자적으로 실행할 수 있습니다.
- 비동기 처리는 응답속도를 개선하지만 메시지 내구성과 장애 복구 전략이 필요합니다.
- Kafka DLT는 실패 메시지를 보관하는 것만으로 끝나지 않고 조회, 원인 확인 및 안전한 Replay 기능이 함께 필요합니다.
- 캐시는 Hit Ratio뿐 아니라 Stampede, Stale Data 및 갱신 실패까지 고려해야 합니다.
- Redis와 DB를 함께 사용하면 불일치를 전제로 탐지, 대사 및 복구 절차를 설계해야 합니다.
