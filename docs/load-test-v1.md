# Load Test v1 - Modular Monolith with Outbox

## 1. 목적

- Outbox 도입 후에도 남는 relay/process coupling을 측정한다.
- 댓글 API latency뿐 아니라 outbox backlog, 처리 지연, handler 간 영향을 측정한다.
- 댓글 API p95/p99가 크게 늘지 않는 경우는 Outbox가 트랜잭션 경계를 잘 분리하고 있다는 신호로 해석한다.
- 대신 notification handler 지연 시 같은 애플리케이션/relay 프로세스 내부에서 backlog, drain time, search 처리 지연이 어떻게 변하는지 본다.

## 2. 테스트 환경

### 실행 환경

- 애플리케이션: Modular Monolith + Outbox
- 실행 프로필: `local,loadtest`
- 댓글 작성 API: `POST /api/v1/posts/{postId}/comments`
- 인증 방식: JWT Bearer

### DB

- MySQL 8.x
- `outbox_event` 테이블 사용
- 현재 실제 outbox 상태 enum:
  - `PENDING`
  - `PROCESSING`
  - `SUCCESS`
  - `FAILED`

문서에서 `processed delay`라고 표현하더라도 실제 완료 상태는 `SUCCESS`를 기준으로 집계한다.

### 테스트 데이터

`loadtest` 프로필에서 애플리케이션 시작 시 아래 고정 데이터가 idempotent하게 준비된다.

- 작성자 계정
  - email: `loadtest.author@example.com`
  - password: `password123!`
- 댓글 작성자 계정
  - email: `loadtest.commenter@example.com`
  - password: `password123!`
- 게시글 10건
  - title prefix: `[loadtest] post `

### k6 설정

디렉터리 구조:

- `loadtest/k6/config.js`
- `loadtest/k6/lib/auth.js`
- `loadtest/k6/lib/comment.js`
- `loadtest/k6/lib/post.js`
- `loadtest/k6/scenarios/comment-baseline.js`
- `loadtest/k6/scenarios/comment-notification-delay-100ms.js`
- `loadtest/k6/scenarios/comment-notification-delay-300ms.js`
- `loadtest/k6/scenarios/mixed-outbox.js`

기본 환경변수:

- `BASE_URL` 기본값: `http://localhost:8080/api/v1`
- `VUS` 기본값: `10`
- `DURATION` 기본값: `30s`

### Outbox scheduler 설정

현재 구현 기준:

- `OutboxEventScheduler` polling 주기: `fixedDelay = 3000ms`
- 한 번에 최대 `100`건 처리
- 검색 outbox 구분 기준: `aggregateType = POST`
- notification outbox 구분 기준: `aggregateType = COMMENT`

## 3. 측정 지표

- comment API p50 / p95 / p99
- comment API throughput
- outbox backlog
  - `status in ('PENDING', 'PROCESSING')`
- outbox failed count
  - `status = 'FAILED'`
- retry count 합계
- outbox `createdAt -> processedAt` 처리 지연
  - notification outbox: `aggregateType = 'COMMENT'`
  - search outbox: `aggregateType = 'POST'`
- notification 생성 지연
- relay drain time

## 4. 실행 방법

### 4.1 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local,loadtest'
```

delay 주입이 필요한 경우:

```bash
./gradlew bootRun --args='--spring.profiles.active=local,loadtest --app.notification.outbox.delay-ms=100'
./gradlew bootRun --args='--spring.profiles.active=local,loadtest --app.notification.outbox.delay-ms=300'
```

운영 프로필에서는 delay injector가 동작하지 않는다.

### 4.2 k6 명령어

baseline:

```bash
k6 run loadtest/k6/scenarios/comment-baseline.js
```

notification delay 100ms:

```bash
k6 run loadtest/k6/scenarios/comment-notification-delay-100ms.js
```

notification delay 300ms:

```bash
k6 run loadtest/k6/scenarios/comment-notification-delay-300ms.js
```

mixed outbox:

```bash
k6 run loadtest/k6/scenarios/mixed-outbox.js
```

환경변수 예시:

```bash
k6 run -e BASE_URL=http://localhost:8080/api/v1 -e VUS=20 -e DURATION=60s loadtest/k6/scenarios/comment-baseline.js
```

### 4.3 k6 확인

k6가 설치되어 있다면 아래로 버전 확인:

```bash
k6 version
```

## 5. SQL 측정 쿼리

### 5.1 backlog count

```sql
select count(*) as backlog_count
from outbox_event
where status in ('PENDING', 'PROCESSING');
```

### 5.2 failed count

```sql
select count(*) as failed_count
from outbox_event
where status = 'FAILED';
```

### 5.3 retry count 합계

```sql
select coalesce(sum(retry_count), 0) as total_retry_count
from outbox_event;
```

### 5.4 notification outbox 처리 지연

```sql
select
    avg(timestampdiff(microsecond, created_at, processed_at)) / 1000 as avg_delay_ms,
    max(timestampdiff(microsecond, created_at, processed_at)) / 1000 as max_delay_ms
from outbox_event
where aggregate_type = 'COMMENT'
  and status = 'SUCCESS'
  and processed_at is not null;
```

### 5.5 search outbox 처리 지연

```sql
select
    avg(timestampdiff(microsecond, created_at, processed_at)) / 1000 as avg_delay_ms,
    max(timestampdiff(microsecond, created_at, processed_at)) / 1000 as max_delay_ms
from outbox_event
where aggregate_type = 'POST'
  and status = 'SUCCESS'
  and processed_at is not null;
```

### 5.6 notification 생성 지연

현재 구현에서는 notification outbox 이벤트의 `aggregate_id`와 생성된 알림의 `target_id`를 comment id로 연결할 수 있다.

```sql
select
    avg(timestampdiff(microsecond, oe.created_at, n.created_at)) / 1000 as avg_notification_latency_ms,
    max(timestampdiff(microsecond, oe.created_at, n.created_at)) / 1000 as max_notification_latency_ms
from outbox_event oe
join notifications n
  on n.target_id = oe.aggregate_id
where oe.aggregate_type = 'COMMENT'
  and oe.status = 'SUCCESS';
```

### 5.7 aggregateType별 backlog

```sql
select aggregate_type, status, count(*) as count
from outbox_event
group by aggregate_type, status
order by aggregate_type, status;
```

### 5.8 drain time 측정 방법

1. 부하 테스트 종료 시각을 기록한다.
2. 아래 쿼리를 주기적으로 실행한다.
3. `backlog_count = 0`이 되는 시각까지의 차이를 drain time으로 기록한다.

```sql
select count(*) as backlog_count
from outbox_event
where status in ('PENDING', 'PROCESSING');
```

## 6. 시나리오별 결과

### 6.1 baseline

- 설정:
- 실행 명령:

| metric | value |
|---|---|
| comment p50 |  |
| comment p95 |  |
| comment p99 |  |
| throughput |  |
| comment backlog peak |  |
| post backlog peak |  |
| notification delay avg/p95 |  |
| search delay avg/p95 |  |
| drain time |  |

- 해석:

### 6.2 notification delay 100ms

- 설정:
- 실행 명령:

| metric | value |
|---|---|
| comment p50 |  |
| comment p95 |  |
| comment p99 |  |
| throughput |  |
| comment backlog peak |  |
| post backlog peak |  |
| notification delay avg/p95 |  |
| search delay avg/p95 |  |
| drain time |  |

- 해석:

### 6.3 notification delay 300ms

- 설정:
- 실행 명령:

| metric | value |
|---|---|
| comment p50 |  |
| comment p95 |  |
| comment p99 |  |
| throughput |  |
| comment backlog peak |  |
| post backlog peak |  |
| notification delay avg/p95 |  |
| search delay avg/p95 |  |
| drain time |  |

- 해석:

### 6.4 mixed outbox

- 설정:
- 실행 명령:

| metric | value |
|---|---|
| comment p50 |  |
| comment p95 |  |
| comment p99 |  |
| throughput |  |
| comment backlog peak |  |
| post backlog peak |  |
| notification delay avg/p95 |  |
| search delay avg/p95 |  |
| drain time |  |

- 해석:

## 7. 해석 기준

- 댓글 API latency가 크게 늘지 않는다면, Outbox가 트랜잭션 경계를 보호하고 있다는 근거로 본다.
- 반대로 notification handler delay가 증가할수록 `COMMENT` backlog, notification 처리 지연, drain time이 증가하면 relay/process coupling이 남아 있다는 뜻이다.
- mixed 시나리오에서 `POST` aggregate 처리 지연까지 함께 증가하면 search와 notification이 같은 relay 프로세스를 공유한다는 영향 근거가 된다.
- v2 비교에서는 RabbitMQ + notification-service 분리 후 같은 지표를 다시 측정한다.

## 8. Known Limitations

- Outbox로 댓글 API 트랜잭션은 보호된다.
- 하지만 search와 notification outbox 처리는 아직 같은 애플리케이션/relay 프로세스 안에서 처리된다.
- notification handler가 느려질 경우 backlog, 처리 지연, drain time이 증가할 수 있다.
- mixed 시나리오에서 search outbox 처리 지연이 함께 증가하면 shared relay contention의 근거가 된다.
- v2에서는 RabbitMQ + notification-service로 전달 경계와 처리 단위를 분리해 비교할 예정이다.
