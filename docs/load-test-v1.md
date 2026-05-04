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
- k6 실행 방식: Docker (`grafana/k6:latest`)
- DB 측정 방식: Docker MySQL client (`mysql:8`)

### DB / 부가 서비스

- MySQL: `127.0.0.1:3306/blog`
- Redis: `127.0.0.1:6379`
- Elasticsearch: `127.0.0.1:9200`
- Spring Boot Docker Compose 자동 연동은 `--spring.docker.compose.enabled=false`로 비활성화하고, 이미 떠 있는 로컬 서비스를 사용했다.

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

실제 실행값:

- `BASE_URL=http://host.docker.internal:8080/api/v1`
- `VUS=2`
- `DURATION=10s`

이번 측정에서는 10 VUs / 30s 기본 템플릿보다 낮은 부하를 사용했다. 이유는 local relay 기준에서 drain time이 급격히 길어져, baseline/100ms/300ms/mixed 4개 시나리오를 모두 같은 세션 안에서 비교 가능한 수준으로 맞추기 위해서다.

### Outbox scheduler 설정

현재 구현 기준:

- `OutboxEventScheduler` polling 주기: `fixedDelay = 3000ms`
- 한 번에 최대 `100`건 처리
- 검색 outbox 구분 기준: `aggregateType = POST`
- notification outbox 구분 기준: `aggregateType = COMMENT`
- 실제 완료 상태 enum: `SUCCESS`

문서에서 `processed delay`라고 표현하더라도 실제 완료 상태는 `SUCCESS`를 기준으로 집계한다.

## 3. 측정 지표

- comment API p50 / p95 / p99
- comment API throughput
- error rate
- outbox backlog peak
- outbox failed count
- retry count 합계
- outbox `createdAt -> processedAt` 처리 지연
  - notification outbox: `aggregateType = 'COMMENT'`
  - search outbox: `aggregateType = 'POST'`
- relay drain time

## 4. 실행 방법

### 4.1 애플리케이션 실행

baseline:

```bash
java -jar build/libs/blog-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local,loadtest \
  --spring.docker.compose.enabled=false \
  --app.notification.outbox.delay-ms=0
```

100ms delay:

```bash
java -jar build/libs/blog-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local,loadtest \
  --spring.docker.compose.enabled=false \
  --app.notification.outbox.delay-ms=100
```

300ms delay:

```bash
java -jar build/libs/blog-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local,loadtest \
  --spring.docker.compose.enabled=false \
  --app.notification.outbox.delay-ms=300
```

### 4.2 k6 명령어

baseline:

```bash
docker run --rm \
  -v <repo>/loadtest:/scripts \
  -v <results-dir>:/results \
  grafana/k6:latest run \
  -e BASE_URL=http://host.docker.internal:8080/api/v1 \
  -e VUS=2 \
  -e DURATION=10s \
  /scripts/k6/scenarios/comment-baseline.js
```

notification delay 100ms:

```bash
docker run --rm \
  -v <repo>/loadtest:/scripts \
  -v <results-dir>:/results \
  grafana/k6:latest run \
  -e BASE_URL=http://host.docker.internal:8080/api/v1 \
  -e VUS=2 \
  -e DURATION=10s \
  /scripts/k6/scenarios/comment-notification-delay-100ms.js
```

notification delay 300ms:

```bash
docker run --rm \
  -v <repo>/loadtest:/scripts \
  -v <results-dir>:/results \
  grafana/k6:latest run \
  -e BASE_URL=http://host.docker.internal:8080/api/v1 \
  -e VUS=2 \
  -e DURATION=10s \
  /scripts/k6/scenarios/comment-notification-delay-300ms.js
```

mixed outbox:

```bash
docker run --rm \
  -v <repo>/loadtest:/scripts \
  -v <results-dir>:/results \
  grafana/k6:latest run \
  -e BASE_URL=http://host.docker.internal:8080/api/v1 \
  -e VUS=2 \
  -e DURATION=10s \
  /scripts/k6/scenarios/mixed-outbox.js
```

### 4.3 SQL 측정 쿼리

backlog count:

```sql
select count(*) as backlog_count
from outbox_event
where status in ('PENDING', 'PROCESSING');
```

failed count:

```sql
select count(*) as failed_count
from outbox_event
where status = 'FAILED';
```

retry count 합계:

```sql
select coalesce(sum(retry_count), 0) as total_retry_count
from outbox_event;
```

notification outbox 처리 지연:

```sql
select
  count(*) as row_count,
  round(avg(timestampdiff(microsecond, created_at, processed_at)) / 1000, 3) as avg_ms,
  round(max(timestampdiff(microsecond, created_at, processed_at)) / 1000, 3) as max_ms
from outbox_event
where aggregate_type = 'COMMENT'
  and status = 'SUCCESS'
  and processed_at is not null;
```

search outbox 처리 지연:

```sql
select
  count(*) as row_count,
  round(avg(timestampdiff(microsecond, created_at, processed_at)) / 1000, 3) as avg_ms,
  round(max(timestampdiff(microsecond, created_at, processed_at)) / 1000, 3) as max_ms
from outbox_event
where aggregate_type = 'POST'
  and status = 'SUCCESS'
  and processed_at is not null;
```

drain time:

1. 부하 테스트 종료 시각을 기록한다.
2. 아래 쿼리로 scenario 시작 시점 이후 backlog가 0이 되는 시점을 기다린다.
3. 종료 시각부터 backlog 0까지의 차이를 drain time으로 기록한다.

```sql
select coalesce(sum(case when status in ('PENDING', 'PROCESSING') then 1 else 0 end), 0) as backlog_count
from outbox_event
where created_at >= :scenarioStart;
```

## 5. 시나리오별 결과

### 5.1 baseline

- 설정:
  - `delay-ms=0`
  - `VUS=2`
  - `DURATION=10s`
- 실행 명령:
  - app: `java -jar build/libs/blog-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,loadtest --spring.docker.compose.enabled=false --app.notification.outbox.delay-ms=0`
  - k6: `docker run --rm ... /scripts/k6/scenarios/comment-baseline.js`

| metric | value |
|---|---:|
| comment request count | 763 |
| comment p50 | 23.624 ms |
| comment p95 | 38.055 ms |
| comment p99 | 52.145 ms |
| throughput | 61.447 req/s |
| error rate | 0.000% |
| comment backlog peak | 621 |
| post backlog peak | 0 |
| total backlog peak | 621 |
| COMMENT outbox delay avg | 17,837.844 ms |
| COMMENT outbox delay max | 33,609.391 ms |
| POST outbox delay avg | N/A |
| POST outbox delay max | N/A |
| FAILED count | 0 |
| retry count | 0 |
| drain time | 32.051 s |

- 해석:
  - 댓글 API latency는 낮게 유지됐다.
  - 반면 relay는 댓글 종료 후에도 약 32초 동안 backlog를 비우는 데 시간이 필요했다.
  - Outbox 덕분에 API 트랜잭션은 보호되지만, relay 처리 지연은 이미 눈에 띄게 존재한다.

### 5.2 notification delay 100ms

- 설정:
  - `delay-ms=100`
  - `VUS=2`
  - `DURATION=10s`
- 실행 명령:
  - app: `java -jar build/libs/blog-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,loadtest --spring.docker.compose.enabled=false --app.notification.outbox.delay-ms=100`
  - k6: `docker run --rm ... /scripts/k6/scenarios/comment-notification-delay-100ms.js`

| metric | value |
|---|---:|
| comment request count | 809 |
| comment p50 | 22.951 ms |
| comment p95 | 36.763 ms |
| comment p99 | 47.372 ms |
| throughput | 65.945 req/s |
| error rate | 0.000% |
| comment backlog peak | 763 |
| post backlog peak | 0 |
| total backlog peak | 763 |
| COMMENT outbox delay avg | 61,456.207 ms |
| COMMENT outbox delay max | 121,494.905 ms |
| POST outbox delay avg | N/A |
| POST outbox delay max | N/A |
| FAILED count | 0 |
| retry count | 0 |
| drain time | 120.798 s |

- 해석:
  - 댓글 API p95/p99는 baseline과 유사했다.
  - 하지만 backlog peak, outbox 처리 지연, drain time은 모두 크게 증가했다.
  - `100ms` 지연만으로도 relay 후처리 비용이 급격히 커지는 것을 확인했다.

### 5.3 notification delay 300ms

- 설정:
  - `delay-ms=300`
  - `VUS=2`
  - `DURATION=10s`
- 실행 명령:
  - app: `java -jar build/libs/blog-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,loadtest --spring.docker.compose.enabled=false --app.notification.outbox.delay-ms=300`
  - k6: `docker run --rm ... /scripts/k6/scenarios/comment-notification-delay-300ms.js`

| metric | value |
|---|---:|
| comment request count | 842 |
| comment p50 | 21.802 ms |
| comment p95 | 35.604 ms |
| comment p99 | 48.016 ms |
| throughput | 68.426 req/s |
| error rate | 0.000% |
| comment backlog peak | 821 |
| post backlog peak | 0 |
| total backlog peak | 821 |
| COMMENT outbox delay avg | 148,016.555 ms |
| COMMENT outbox delay max | 295,714.415 ms |
| POST outbox delay avg | N/A |
| POST outbox delay max | N/A |
| FAILED count | 0 |
| retry count | 0 |
| drain time | 294.471 s |

- 해석:
  - 댓글 API latency는 여전히 크게 악화되지 않았다.
  - 하지만 drain time은 약 4.9분으로 늘었고, COMMENT outbox 평균 지연도 약 148초까지 증가했다.
  - 이 값은 “API latency는 보호되지만 relay coupling은 남아 있다”는 점을 수치로 보여준다.

### 5.4 mixed outbox

- 설정:
  - `delay-ms=300`
  - `VUS=2`
  - `DURATION=10s`
  - 시나리오 구성: 댓글 작성 + 게시글 수정 혼합
- 실행 명령:
  - app: `java -jar build/libs/blog-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,loadtest --spring.docker.compose.enabled=false --app.notification.outbox.delay-ms=300`
  - k6: `docker run --rm ... /scripts/k6/scenarios/mixed-outbox.js`

| metric | value |
|---|---:|
| comment request count | 554 |
| comment p50 | 22.142 ms |
| comment p95 | 37.620 ms |
| comment p99 | 43.791 ms |
| throughput | 44.482 req/s |
| error rate | 0.000% |
| comment backlog peak | 530 |
| post backlog peak | 228 |
| total backlog peak | 758 |
| COMMENT outbox delay avg | 103,512.850 ms |
| COMMENT outbox delay max | 205,506.909 ms |
| POST outbox delay avg | 103,636.263 ms |
| POST outbox delay max | 201,914.834 ms |
| FAILED count | 0 |
| retry count | 0 |
| drain time | 204.648 s |

- 해석:
  - mixed 상황에서도 댓글 API latency는 낮게 유지됐다.
  - 대신 `POST` outbox 평균 지연이 `COMMENT` outbox 평균 지연과 거의 같은 수준까지 올라갔다.
  - 이는 느린 notification handler가 search outbox 처리에도 영향을 주는 shared relay contention 근거다.

## 6. 해석

- 댓글 API p95/p99는 네 시나리오 모두 큰 폭으로 증가하지 않았다.
- 이는 Outbox가 댓글 트랜잭션 경계를 분리해, notification 처리 지연이 API 응답 시간을 직접 밀어 올리지 않도록 막고 있음을 보여준다.
- 하지만 API latency와 별개로 relay 후처리 측면에서는 분명한 coupling이 남아 있었다.
- baseline에서도 COMMENT outbox 평균 처리 지연은 약 `17.8s`, drain time은 `32.1s`였다.
- `delay-ms=100`에서는 COMMENT outbox 평균 지연이 약 `61.5s`, drain time이 약 `120.8s`로 증가했다.
- `delay-ms=300`에서는 COMMENT outbox 평균 지연이 약 `148.0s`, drain time이 약 `294.5s`까지 증가했다.
- mixed 시나리오에서는 `POST` outbox 평균 지연이 `103.6s`로 측정돼, notification handler가 느려질 때 search relay도 함께 밀리는 것을 확인했다.
- 즉 v1 구조는 “원본 트랜잭션 보호”에는 성공했지만, “relay 처리 단위 분리”에는 아직 도달하지 못했다.
- 이 결과는 v2에서 RabbitMQ + notification-service로 전달 경계와 처리 단위를 분리해 비교해야 할 이유를 만든다.

## 7. Known Limitations

- Outbox로 댓글 API 트랜잭션은 보호된다.
- 하지만 search와 notification outbox 처리는 아직 같은 애플리케이션/relay 프로세스 안에서 처리된다.
- notification handler가 느려질 경우 backlog, 처리 지연, drain time이 증가할 수 있다.
- mixed 시나리오에서 search outbox 처리 지연이 함께 증가하면 shared relay contention의 근거가 된다.
- v2에서는 RabbitMQ + notification-service로 전달 경계와 처리 단위를 분리해 비교할 예정이다.

## 8. Measurement Limitations

- `VUS=2`, `DURATION=10s`는 절대 성능 측정이 아니라 relay contention 검증용 최소 부하다.
- 이번 측정의 목적은 API 최대 처리량 산정이 아니라 outbox backlog, 처리 지연, drain time, mixed 시나리오의 `POST` 지연을 비교 가능한 기준선으로 남기는 것이다.
- raw k6 JSON 결과와 중간 backlog 샘플 파일은 이번 커밋에 포함하지 않았다. v2 비교 측정부터는 raw 결과도 함께 보존할 예정이다.
- baseline에서도 COMMENT outbox 평균 처리 지연이 `17.8s`였으며, 이는 `fixedDelay=3s` polling, batch size `100`, 요청량에 따른 backlog/drain 패턴의 영향을 함께 받는다.
- 따라서 이 문서의 핵심 해석은 댓글 API latency 자체보다, 느린 notification handler가 relay 내부 처리 큐에 어떤 영향을 주는지에 있다.
