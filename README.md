# Blog Service — MySQL·ES 정합성 + 실시간 인기글 + 다중 검색 전략

> **Spring Boot 4 + React 19 기반 풀스택 블로그 서비스**
> 단순 CRUD를 넘어, **분산 환경에서의 데이터 정합성**, **실시간 랭킹**, **다중 검색 전략**, **엔터프라이즈급 관찰가능성**을 구현한 포트폴리오 프로젝트입니다.
>
> v1 Outbox load test 문서: [docs/load-test-v1.md](docs/load-test-v1.md) — Outbox로 트랜잭션 경계는 분리했지만, relay/process coupling은 별도 측정 대상으로 남아 있습니다.

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.0-6DB33F?style=flat-square&logo=springboot)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-9.2.1-005571?style=flat-square&logo=elasticsearch)

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [시스템 아키텍처](#3-시스템-아키텍처)
4. [핵심 기능 목록](#4-핵심-기능-목록)
5. [전체 API 목록](#5-전체-api-목록)
6. [핵심 아키텍처 설계](#6-핵심-아키텍처-설계)
   - 6.1 [Outbox 패턴 — MySQL↔ES 정합성 보장](#61-outbox-패턴--mysqles-정합성-보장)
   - 6.2 [인기글 집계 — Redis ZSet + Spring Batch](#62-인기글-집계--redis-zset--spring-batch)
   - 6.3 [다중 검색 전략](#63-다중-검색-전략)
   - 6.4 [조회수 중복 방지 — Redis SETNX](#64-조회수-중복-방지--redis-setnx)
   - 6.5 [JWT 인증 전략](#65-jwt-인증-전략)
   - 6.6 [ES Sync Repair Batch](#66-es-sync-repair-batch)
7. [관찰가능성 (Observability)](#7-관찰가능성-observability)
8. [테스트 전략](#8-테스트-전략)
9. [Load Test Findings](#9-load-test-findings)
10. [실행 방법](#10-실행-방법)
11. [프론트엔드](#11-프론트엔드)

---

## 1. 프로젝트 개요

### 이 프로젝트를 만든 이유

블로그 서비스는 단순해 보이지만, **실제 운영 환경에서 자주 맞닥뜨리는 어려운 문제들**을 담고 있습니다.

| 문제 | 이 프로젝트의 해결 방식 |
|---|---|
| 검색 인덱스가 원본 DB와 어긋나면? | Outbox 패턴 + Repair Batch로 최종 일관성 보장 |
| 조회수가 새로고침할 때마다 오르면? | Redis SETNX 기반 24시간 중복 방지 |
| 인기글을 실시간으로 집계하려면? | Redis ZSet 점수 누적 → Spring Batch 일괄 정산 |
| 검색 방식이 다양하면 어떻게 확장하나? | MySQL 4종(infix/prefix/fulltext/boolean) + ES 전략 패턴 |
| 서비스 내부를 들여다보고 싶다면? | OpenTelemetry → Jaeger, Micrometer → Prometheus → Grafana |

### 핵심 설계 철학

- **원본(MySQL)과 인덱스(Elasticsearch)의 분리**: 검색 성능을 위해 ES를 별도 인덱스로 두되, 반영 누락 문제를 Outbox + Repair 이중 안전망으로 해결
- **이벤트 기반 비동기 처리**: Post 변경과 ES 반영을 트랜잭션으로 강결합하지 않고, Outbox 이벤트를 통해 느슨하게 연결
- **실시간 + 배치 병행**: 인기글 점수는 실시간으로 Redis에 누적하고, 정산은 Spring Batch로 안정적으로 처리

---

## 2. 기술 스택

### Backend

| 분류 | 기술 | 버전 | 선택 이유 |
|---|---|---|---|
| Language | Java | 17 | LTS, Record/Sealed class 활용 |
| Framework | Spring Boot | 4.0.0 | Jakarta EE 11, 최신 생태계 |
| ORM | Spring Data JPA / Hibernate | - | 객체 중심 도메인 모델링 |
| DB | MySQL | 8.0 | Fulltext Index, ACID 트랜잭션 |
| Cache | Redis | 7 | ZSet 기반 랭킹, SETNX 중복 방지 |
| Search | Elasticsearch | 9.2.1 | 대용량 전문 검색, 역색인 |
| Batch | Spring Batch | - | 청크 기반 대량 데이터 처리 |
| Auth | JJWT | 0.12.3 | Stateless JWT 인증 |
| Observability | OpenTelemetry + Micrometer | - | 표준 계측, 벤더 중립 |
| Tracing | Jaeger | 1.76.0 | 분산 추적 시각화 |
| Metrics | Prometheus + Grafana | - | 메트릭 수집 및 대시보드 |
| API Docs | springdoc-openapi | 3.0.0 | Swagger UI 자동 생성 |
| Build | Gradle | 8.7 | 의존성 관리 |
| Test | JUnit 5 + Testcontainers | - | 실제 인프라 기반 통합 테스트 |

### Frontend

| 분류 | 기술 | 버전 |
|---|---|---|
| Framework | React | 19.2.3 |
| Language | TypeScript | 5.8.2 |
| Build Tool | Vite | 6.2.0 |
| Routing | React Router | 7.11.0 |
| State | Zustand | 5.0.9 |
| HTTP | Axios | 1.13.2 |
| Markdown | react-markdown + remark-gfm | - |
| Styling | Tailwind CSS (CDN) | - |

---

## 3. 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client (React 19)                        │
│        Zustand 상태관리 │ Axios 인터셉터 (JWT 자동 갱신)           │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTP /api/v1/**
┌────────────────────────▼────────────────────────────────────────┐
│                    Spring Boot 4 API Server                      │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────┐  │
│  │  Auth    │  │  Post    │  │ Comment  │  │    Search      │  │
│  │ /auth/** │  │/posts/** │  │/comments │  │  /search/**    │  │
│  └──────────┘  └────┬─────┘  └──────────┘  └───────┬────────┘  │
│                     │                               │            │
│              ┌──────▼──────┐               ┌───────▼────────┐  │
│              │  OutboxEvent │               │ PostSearchSync  │  │
│              │  (PENDING)   │◄──────────────│  Service       │  │
│              └──────┬──────┘               └────────────────┘  │
│                     │ 3초마다 polling                            │
│              ┌──────▼──────────────┐                            │
│              │  OutboxEventScheduler│                            │
│              └─────────────────────┘                            │
└──────────┬──────────┬──────────┬────────────────────────────────┘
           │          │          │
    ┌──────▼──┐ ┌─────▼──┐ ┌────▼──────────────┐
    │  MySQL  │ │ Redis  │ │  Elasticsearch     │
    │  (원본)  │ │(캐시/  │ │  (검색 인덱스)      │
    │         │ │ 랭킹)  │ │                   │
    └─────────┘ └────────┘ └───────────────────┘
           │
    ┌──────▼────────────────────────────────────────┐
    │            관찰가능성 인프라                     │
    │  OpenTelemetry → Jaeger    (분산 추적)          │
    │  Micrometer → Prometheus → Grafana (메트릭)    │
    └───────────────────────────────────────────────┘
```

### 인기글 집계 흐름

```
사용자 행동 (조회·좋아요·댓글)
        ↓
  PopularEventService
  (VIEW +1점 / LIKE +3점 / COMMENT +5점)
        ↓
  Redis ZSet  post:score:daily:{date}
        ↓
  Spring Batch (매일 자정)
        ↓
  popular_posts_daily (MySQL)
```

### Outbox 이벤트 상태 전이

```
게시글 변경 (CREATE/UPDATE/DELETE)
        ↓ 같은 트랜잭션
  OutboxEvent [PENDING]
        ↓ 3초마다 스케줄러
  OutboxEvent [PROCESSING]
        ↓ ES 동기화 성공          ↓ 실패
  OutboxEvent [SUCCESS]    OutboxEvent [FAILED → PENDING]
                                    ↓ 지수 백오프 (10s→30s→60s→5min)
                              최대 5회 재시도
```

---

## 4. 핵심 기능 목록

### 게시글
- 작성 / 수정 / 소프트 삭제 / 목록 조회(Slice 페이징) / 상세 조회
- 마크다운 지원, 태그 다중 선택, 시리즈 분류
- 조회수 24시간 중복 방지 (Redis SETNX)

### 댓글
- 작성 / 수정 / 소프트 삭제 / 목록 조회
- 삭제된 댓글은 "삭제된 댓글입니다." 표시 유지

### 좋아요
- 토글 방식 (추가/취소)
- 좋아요 이벤트 → 인기글 점수 반영

### 검색 (5가지 엔드포인트)
- MySQL Infix / Prefix / Fulltext / Fulltext-Boolean
- Elasticsearch 전문 검색
- Offset 기반 페이징

### 인기글
- 일별 실시간 랭킹 (Redis ZSet)
- 일별 아카이브 랭킹 (MySQL, 배치 결과)
- Spring Batch 수동 실행 API

### 인증
- 회원가입 / 로그인 / 내 정보 조회
- AccessToken (1시간) + RefreshToken (14일, HttpOnly Cookie)
- 프론트엔드 자동 토큰 갱신 (Axios 인터셉터)

### 관찰가능성
- OpenTelemetry → Jaeger 분산 추적
- Micrometer → Prometheus → Grafana 메트릭
- Swagger UI (`/api/v1/swagger-ui/index.html`)

---

## 5. 전체 API 목록

> **Base URL**: `http://localhost:8080/api/v1`
> **인증**: `Authorization: Bearer {accessToken}` 헤더

### Auth

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `POST` | `/auth/signup` | ✗ | 회원가입 |
| `POST` | `/auth/login` | ✗ | 로그인 (AccessToken + RefreshToken 발급) |
| `GET` | `/auth/me` | ✔ | 내 정보 조회 |

<details>
<summary>요청/응답 예시</summary>

**POST /auth/signup**
```json
// Request
{ "email": "user@example.com", "password": "pass1234", "nickname": "홍길동" }
// Response
{ "success": true, "data": null }
```

**POST /auth/login**
```json
// Request
{ "email": "user@example.com", "password": "pass1234" }
// Response
{ "success": true, "data": { "accessToken": "eyJ...", "refreshToken": "eyJ..." } }
```
</details>

---

### Post

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `GET` | `/posts` | ✗ | 게시글 목록 (Slice 페이징) |
| `GET` | `/posts/{postId}` | ✗ | 게시글 상세 + 조회수 증가 |
| `POST` | `/posts` | ✔ | 게시글 작성 |
| `PUT` | `/posts/{postId}` | ✔ | 게시글 수정 |
| `DELETE` | `/posts/{postId}` | ✔ | 게시글 삭제 (soft delete) |
| `POST` | `/posts/{postId}/like` | ✔ | 좋아요 토글 |

<details>
<summary>요청/응답 예시</summary>

**GET /posts?page=0&size=10&sort=createdAt,DESC**
```json
{
  "content": [{
    "postId": 1,
    "title": "Spring Boot 4 입문",
    "author": { "userId": 1, "nickname": "홍길동" },
    "viewCount": 150,
    "likeCount": 12,
    "tags": ["java", "spring"],
    "createdAt": "2026-04-01T10:00:00"
  }],
  "page": 0, "size": 10, "hasNext": true
}
```

**POST /posts**
```json
// Request
{
  "title": "제목",
  "content": "## 내용\n마크다운 지원",
  "tags": ["java", "backend"],
  "seriesId": null
}
// Response
{ "success": true, "data": { "postId": 1 } }
```

**POST /posts/{postId}/like**
```json
// Response
{ "success": true, "data": { "liked": true, "likeCount": 13 } }
```
</details>

---

### Comment

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `GET` | `/posts/{postId}/comments` | ✗ | 댓글 목록 조회 |
| `POST` | `/posts/{postId}/comments` | ✔ | 댓글 작성 |
| `PATCH` | `/comments/{commentId}` | ✔ | 댓글 수정 |
| `DELETE` | `/comments/{commentId}` | ✔ | 댓글 삭제 (soft delete) |

<details>
<summary>요청/응답 예시</summary>

**GET /posts/1/comments**
```json
{
  "success": true,
  "data": [{
    "id": 1,
    "postId": 1,
    "author": { "id": 1, "nickname": "홍길동" },
    "content": "좋은 글이에요!",
    "deleted": false,
    "mine": true,
    "createdAt": "2026-04-01T10:05:00"
  }]
}
```
</details>

---

### Search

| Method | Path | 인증 | 검색 방식 |
|---|---|---|---|
| `GET` | `/search/posts` | ✗ | MySQL Infix (기본, 호환용) |
| `GET` | `/search/posts/infix` | ✗ | MySQL LIKE '%keyword%' |
| `GET` | `/search/posts/prefix` | ✗ | MySQL LIKE 'keyword%' |
| `GET` | `/search/posts/fulltext` | ✗ | MySQL MATCH NATURAL LANGUAGE |
| `GET` | `/search/posts/fulltext-boolean` | ✗ | MySQL MATCH BOOLEAN MODE |
| `GET` | `/search/posts/es` | ✗ | Elasticsearch |

**공통 쿼리 파라미터**: `keyword`, `offset` (기본 0), `limit` (기본 20)

<details>
<summary>응답 예시</summary>

```json
{
  "success": true,
  "data": {
    "items": [{
      "postId": 1,
      "title": "Spring Boot 검색 구현",
      "contentPreview": "Elasticsearch를 이용한 전문 검색...",
      "author": { "authorId": 1, "authorNickname": "홍길동" },
      "viewCount": 300,
      "likeCount": 25,
      "createdAt": "2026-04-01T09:00:00"
    }],
    "offset": 0,
    "limit": 20,
    "hasNext": false
  }
}
```
</details>

---

### Popular Post

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `GET` | `/popular-posts/daily` | ✗ | 일별 인기글 (Redis 실시간) |
| `GET` | `/popular-posts/daily/archive` | ✗ | 일별 인기글 (MySQL 아카이브) |
| `POST` | `/popular-posts/popular` | ✗ | 인기글 배치 수동 실행 (테스트용) |

**쿼리 파라미터**: `date` (yyyy-MM-dd, 기본 오늘), `limit` (기본 10)

---

### Series & Batch

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| `GET` | `/series` | ✔ | 내 시리즈 목록 조회 |
| `POST` | `/test/batch/repair-es` | ✗ | ES Sync Repair 배치 수동 실행 |

---

## 6. 핵심 아키텍처 설계

### 6.1 Outbox 패턴 — MySQL↔ES 정합성 보장

#### 문제 상황

```
PostService.createPost() {
    post = postRepository.save(post);  // ①
    esClient.index(document);          // ② ← 여기서 장애나면?
}
```

①은 커밋됐는데 ②가 실패하면 MySQL엔 있고 ES엔 없는 **데이터 불일치** 발생. 재시작 후에도 이 이벤트는 사라져 복구 불가.

#### 해결: Outbox 패턴

**같은 트랜잭션** 안에서 Post 저장과 OutboxEvent 기록을 원자적으로 처리합니다.

```java
// PostServiceImpl.java
@Transactional
public PostCreateResponse createPost(PostPublishedDto dto, String username) {
    Post saved = postRepository.save(post);         // ① Post 저장
    postOutboxService.createCreatedEvent(saved);    // ② Outbox 기록 (같은 TX)
    return PostCreateResponse.from(saved);
}
```

**OutboxEvent 스키마**:

| 필드 | 타입 | 설명 |
|---|---|---|
| `id` | Long | PK |
| `aggregateType` | String | "POST" |
| `aggregateId` | Long | postId |
| `eventType` | Enum | CREATED / UPDATED / DELETED |
| `payload` | JSON | `{ postId, version }` |
| `status` | Enum | PENDING → PROCESSING → SUCCESS / FAILED |
| `retryCount` | int | 최대 5회 |
| `nextRetryAt` | LocalDateTime | 지수 백오프 계산값 |

**재시도 지수 백오프**:

```java
private long calculateBackoffSeconds() {
    return switch (this.retryCount) {
        case 1 -> 10;    // 10초
        case 2 -> 30;    // 30초
        case 3 -> 60;    // 60초
        default -> 300;  // 5분
    };
}
```

**처리 흐름**:

```
OutboxEventScheduler (3초마다)
  └─→ PENDING & nextRetryAt ≤ now 이벤트 최대 100개 조회
        └─→ OutboxEventOrchestrator.process(eventId)
              ├─ markProcessing()  (낙관적 상태 전환)
              ├─ DELETED  → ES에서 문서 삭제
              ├─ CREATED/UPDATED → ES에 upsert
              ├─ 성공  → markSuccess()
              └─ 실패  → markFailureAndRequeue() (지수 백오프)
```

**왜 DB 폴링(Pull)이고 메시지 큐(Push)가 아닌가?**

> 추가 인프라(Kafka, RabbitMQ) 없이 MySQL만으로 At-Least-Once 전달을 구현할 수 있기 때문입니다. 메시지 큐를 도입하면 브로커 장애 시 새로운 단일 장애점이 생기고 운영 복잡도가 올라갑니다. 현재 이벤트 처리량은 DB 폴링으로 충분히 감당 가능한 수준이며, 확장이 필요하면 마이그레이션도 어렵지 않습니다.

---

### 6.2 인기글 집계 — Redis ZSet + Spring Batch

#### 설계 목표

- **실시간성**: 조회/좋아요/댓글이 발생하는 즉시 순위에 반영
- **안정성**: Redis 데이터 유실 시 MySQL 아카이브로 복구 가능
- **확장성**: 점수 가중치를 정책 객체(PopularScorePolicy)로 분리해 변경 용이

#### 점수 정책

```java
// PopularScorePolicy.java
public static final double VIEW_SCORE    = 1.0;
public static final double LIKE_SCORE    = 3.0;
public static final double COMMENT_SCORE = 5.0;
```

#### Redis 키 구조

```
post:score:daily:{yyyy-MM-dd}    # 일별 ZSet (TTL: 8일)
post:score:weekly:{yyyy-MM-dd}   # 주별 ZSet (기준: 해당 주 월요일, TTL: 35일)
```

#### 실시간 반영 (Redis ZSet ZINCRBY)

```java
// PopularEventService.java
public void reflectView(Long postId) {
    String dailyKey = PopularKeyGenerator.dailyKey(LocalDate.now());
    redisTemplate.opsForZSet()
        .incrementScore(dailyKey, postId.toString(), VIEW_SCORE);
}
```

#### 배치 정산 (매일 자정)

```
PopularPostBatchScheduler → popularPostAggregationJob
  └─ Step: aggregateDailyPopularPostsStep
       └─ PopularAggregationService.aggregateDaily(targetDate, limit=100)
            ├─ Redis ZSet에서 상위 100개 조회 (ZREVRANGEBYSCORE)
            ├─ popular_posts_daily 테이블 UPSERT (순위, 점수)
            └─ 기존 해당 날짜 데이터 삭제 후 재적재
```

**조회 2-tier 전략**:

```java
// 오늘 ~ 최근: Redis 실시간 데이터
GET /popular-posts/daily          → Redis ZREVRANGEBYSCORE

// 과거 날짜: MySQL 아카이브
GET /popular-posts/daily/archive  → SELECT * FROM popular_posts_daily WHERE target_date = ?
```

---

### 6.3 다중 검색 전략

MySQL의 5가지 검색 방식과 Elasticsearch를 비교하며 성능·정확도 트레이드오프를 확인할 수 있도록 설계했습니다.

| 전략 | 엔드포인트 | SQL/쿼리 | 특징 |
|---|---|---|---|
| **Infix** | `/search/posts/infix` | `LIKE '%keyword%'` | 가장 유연, 인덱스 불가, 느림 |
| **Prefix** | `/search/posts/prefix` | `LIKE 'keyword%'` | 인덱스 활용 가능, 빠름 |
| **Fulltext** | `/search/posts/fulltext` | `MATCH ... IN NATURAL LANGUAGE MODE` | 형태소 분석, 불용어 제거 |
| **Fulltext Boolean** | `/search/posts/fulltext-boolean` | `MATCH ... IN BOOLEAN MODE` | `+필수어 -제외어 *와일드카드` 연산자 지원 |
| **Elasticsearch** | `/search/posts/es` | ES `bool` query | 대용량 최적화, 역색인, 실시간 랭킹 |

#### Elasticsearch 쿼리 구조

```java
// PostSearchEsRepository.java
elasticsearchClient.search(s -> s
    .index(INDEX_NAME)
    .query(q -> q.bool(b -> b
        .must(m -> m.match(mt -> mt
            .field("title").query(keyword)
        ))
        .filter(f -> f.term(t -> t
            .field("poststatus.keyword").value("PUBLISHED")
        ))
    ))
    .sort(sort -> sort
        .field(f -> f.field("createdat").order(SortOrder.Desc))
    )
    .from(offset).size(limit)
, PostSearchDocument.class);
```

검색 노출 정책:
- MySQL 검색은 `PUBLISHED` 이고 `deleted_at is null` 인 게시글만 노출합니다.
- Elasticsearch 검색도 `PUBLISHED` 문서만 필터링합니다.
- `DELETED` 게시글은 삭제 이벤트가 성공하면 ES 문서가 제거됩니다. 삭제 이벤트가 실패하면 stale 문서가 잠시 남을 수 있으며, 현재 구조에서는 retry/repair 경로로 최종 정합성을 맞춥니다.

#### 버전 기반 동기화 (syncVersion)

Post 엔티티와 ES 문서 모두 `version` 필드를 가지며, Repair Batch가 이를 비교해 불일치 시 재동기화합니다.

```
MySQL post.syncVersion == ES document.version  → 동기화 완료
MySQL post.syncVersion != ES document.version  → Repair Batch가 재동기화
```

---

### 6.4 조회수 중복 방지 — Redis SETNX

#### 문제

같은 사용자가 1분 안에 게시글을 10번 열면 조회수가 10 올라가는 것은 부자연스럽습니다.

#### 해결

```java
// RedisPostViewDedupService.java
public boolean shouldIncrease(Long postId, String viewerId) {
    String key = "post:viewed:" + postId + ":" + viewerId;
    Boolean isNew = redisTemplate.opsForValue()
        .setIfAbsent(key, "1", Duration.ofHours(24)); // 24시간 TTL
    return Boolean.TRUE.equals(isNew);
}
```

`viewerId`는 로그인 사용자이면 userId, 비로그인이면 IP+UA 해시값으로 구성됩니다.

#### 조회 시 전체 흐름

```java
// PostServiceImpl.getPost()
boolean shouldIncrease = redisPostViewDedupService.shouldIncrease(postId, viewerId);

if (shouldIncrease) {
    post.increaseViewCount();        // DB 조회수 +1
    post.increaseSyncVersion();      // ES 재동기화 트리거
    postOutboxService.createUpdatedEvent(post);  // Outbox 기록
    popularEventService.reflectView(postId);     // 인기글 점수 반영
}
```

---

### 6.5 JWT 인증 전략

#### 토큰 이중 구조

```
AccessToken
  - 유효기간: 1시간
  - 저장: 클라이언트 메모리 (Zustand)
  - 전달: Authorization: Bearer {token}

RefreshToken
  - 유효기간: 14일
  - 저장: HttpOnly Cookie (path=/api/v1/auth, sameSite=Lax)
  - 목적: AccessToken 만료 시 재발급
```

#### 자동 갱신 흐름 (Frontend Interceptor)

```typescript
// axios.ts — Response Interceptor
if (error.response?.status === 401 && !isRefreshRequest) {
    const refreshRes = await refreshAccessToken();     // Cookie의 RefreshToken으로 재발급
    const newAccessToken = refreshRes.data.data.accessToken;
    useAuthStore.getState().setTokens(newAccessToken); // 메모리 갱신
    return retryOriginalRequest(newAccessToken);       // 원래 요청 재시도
}
```

#### Security 필터 체인

```
JWTFilter (OncePerRequestFilter)
  ↓
  "Authorization: Bearer {token}" 헤더 파싱
  ↓
  토큰 만료 검증 → 만료 시 401 응답
  ↓
  username 추출 → CustomUserDetailsService.loadUserByUsername()
  ↓
  SecurityContext에 인증 정보 설정
```

---

### 6.6 ES Sync Repair Batch

Outbox 처리가 5회 모두 실패하거나, 인프라 장애로 이벤트 자체가 유실된 경우를 대비한 **2차 안전망**입니다.

#### 복구 알고리즘

```java
// SearchIndexRepairService.java
for (Post post : postsUpdatedAfter(from)) {
    PostSearchDocument esDoc = postSearchRepository.findByPostId(post.getPostId());

    if (post.isDeleted()) {
        if (esDoc != null) deleteFromEs();     // stale 문서 삭제
        else skip();
        continue;
    }

    if (esDoc == null) {
        syncToEs();                             // 누락 문서 재색인
    } else if (!post.getSyncVersion().equals(esDoc.getVersion())) {
        syncToEs();                             // 버전 불일치 → 재색인
    } else {
        skip();                                 // 정상
    }
}
```

**배치 파라미터**: `from` (기본값: 7일 전)

**수동 실행 (테스트/운영)**:
```bash
POST /api/v1/test/batch/repair-es?from=2026-03-25T00:00:00
```

---

## 7. 관찰가능성 (Observability)

### 분산 추적 — OpenTelemetry + Jaeger

```java
// TraceHelper.java
public <T> T trace(String name, Supplier<T> supplier) {
    return Observation.createNotStarted(name, observationRegistry)
        .observe(supplier::get);
}
```

**게시글 작성 트레이스 예시**:
```
post.create
  ├─ post.create.find-author       (0.5ms)
  ├─ post.create.find-series       (0.3ms)
  ├─ post.create.attach-tags       (1.2ms)
  ├─ post.create.save              (3.4ms)
  └─ post.create.outbox-created    (0.8ms)
```

**Jaeger UI**: `http://localhost:16686`

---

### 메트릭 — Micrometer + Prometheus + Grafana

```yaml
# application.yml
management:
  server:
    port: 8081
  endpoints:
    web:
      exposure:
        include: health,info,metrics,threaddump,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    distribution:
      percentiles-histogram:
        blog.search.outbox.processing.latency: true
```

**개발 환경 노출 정책**:

- management port는 `8081` 로 앱 포트와 분리
- actuator는 비즈니스 API와 분리된 management 포트에서만 확인
- 현재 통합 테스트 기준 management 포트의 actuator endpoint는 익명 요청에 `401` 을 반환할 수 있으므로, 내부망 접근 또는 인증된 요청 기준으로 확인
- Prometheus scrape 대상은 `prometheus/prometheus.yml` 기준으로 `host.docker.internal:8081/actuator/prometheus`

**주요 메트릭**:

| 메트릭 | 설명 |
|---|---|
| `http_server_requests_seconds_*` | HTTP 요청 지연 (p50, p95, p99) |
| `blog.search.outbox.processing.latency` | Outbox 처리 지연 히스토그램 |
| `blog_search_outbox_pending_count` | 현재 PENDING Outbox 이벤트 수 |
| `blog_search_outbox_failed_count` | 현재 FAILED Outbox 이벤트 수 |
| `blog_search_es_sync_total` | ES 동기화 성공/실패 카운터 |
| `blog_search_outbox_retry_total` | Outbox 재시도 카운터 |
| `jvm_memory_used_bytes` | JVM 힙/메타스페이스 사용량 |
| `hikaricp_connections_*` | DB 커넥션 풀 상태 |

**수동 확인 방법**:

```bash
# 익명 접근 시 401이 나올 수 있음
curl http://localhost:8081/actuator/health

# 인증된 요청 예시
curl -H "Authorization: Bearer <ACCESS_TOKEN>" http://localhost:8081/actuator/prometheus
```

- Prometheus target 확인: `http://localhost:9090/targets`
- Grafana datasource 확인: `http://localhost:3001`

**traceId/spanId 로그 확인**:

- 로그 패턴에 `traceId` / `spanId`를 포함
- `TraceHelper`로 감싼 구간은 Jaeger trace와 로그를 함께 상관관계 확인 가능

```text
2026-05-04 20:10:15.321 INFO  [http-nio-8080-exec-1] [traceId=8f1d... spanId=2ab3...] c.e.b.post.service.PostServiceImpl - ...
```

**운영 환경 권장 정책**:

- `management.server.port`는 내부망 또는 프라이빗 서브넷으로 제한
- `/actuator/prometheus` 외부 공개 금지
- `management.endpoint.health.show-details=always` 는 개발 전용
- 운영에서는 `when_authorized` 또는 `never` 권장
- Grafana/Prometheus는 내부 관측망에서만 접근 권장

**Prometheus**: `http://localhost:9090`
**Grafana**: `http://localhost:3001`

---

### Swagger API 문서

**URL**: `http://localhost:8080/api/v1/swagger-ui/index.html`

전체 API에 `@Tag`, `@Operation`, `@Parameter`, `@SecurityRequirement` 어노테이션 적용 완료.

---

## 8. 테스트 전략

H2 인메모리 DB 대신 **Testcontainers**로 실제 MySQL 8.0, Elasticsearch 9.2.1 컨테이너를 띄워 테스트합니다.
"테스트에선 통과했는데 운영에서 터진다"는 환경 불일치 문제를 원천 차단합니다.

### PostServiceIntegrationTest

```
실제 MySQL 컨테이너 기반

✔ 게시글 생성 시 Post 저장 + OutboxEvent(CREATED) 생성 확인
✔ 게시글 수정 시 syncVersion 증가 + OutboxEvent(UPDATED) 생성 확인
✔ 게시글 삭제 시 soft delete + OutboxEvent(DELETED) 생성 확인
```

### SearchIndexRepairIntegrationTest

```
실제 MySQL + Elasticsearch 컨테이너 기반

✔ MySQL에 있고 ES에 없는 게시글 → 재색인 확인
✔ MySQL에서 삭제됐지만 ES에 남은 stale 문서 → 삭제 확인
✔ syncVersion 불일치 게시글 → ES 재동기화 확인
```

### 테스트 실행

```bash
# 전체 테스트 (Docker 데몬 실행 필요)
./gradlew test

# 특정 클래스만
./gradlew test --tests "com.example.blog.post.service.PostServiceIntegrationTest"
./gradlew test --tests "com.example.blog.search.service.SearchIndexRepairIntegrationTest"
```

---

## 9. Load Test Findings

- 기준 문서: [docs/load-test-v1.md](docs/load-test-v1.md)
- 이번 v1 측정은 `VUS=2`, `DURATION=10s` 조건의 최소 부하로 수행했고, 절대 성능 검증이 아니라 v1과 v2를 비교하기 위한 contention 기준선 확보가 목적이었습니다.
- 댓글 API `p99`는 네 시나리오에서 약 `44~52ms` 범위로 유지됐고, 이는 Outbox가 댓글 작성 트랜잭션과 알림 후처리를 분리한 효과로 해석할 수 있습니다.
- 반면 notification handler에 `300ms` 지연을 주입했을 때 COMMENT outbox 평균 처리 지연은 baseline `17.8s`에서 `148.0s`로 증가했습니다.
- mixed 시나리오에서는 무관한 POST search outbox도 평균 `103.6s` 지연돼, search와 notification이 같은 relay/orchestrator 흐름을 공유하는 영향이 확인됐습니다.
- 따라서 v1의 한계는 API 트랜잭션 결합이 아니라 shared relay contention이며, v2에서는 RabbitMQ + notification-service로 notification 처리를 별도 실행/배포/스케일링 단위로 분리할 예정입니다.

---

## 10. 실행 방법

### 사전 요구사항

- Java 17+
- Docker & Docker Compose
- Node.js 20+ (프론트엔드)

### 1. 저장소 클론

```bash
git clone <repository-url>
cd blog
```

### 2. 인프라 실행

```bash
docker compose up -d
```

| 서비스 | 포트 | 용도 |
|---|---|---|
| MySQL | 3306 | 원본 데이터 |
| Redis | 6379 | 캐시, 랭킹 |
| Elasticsearch | 9200 | 검색 인덱스 |
| Kibana | 5601 | ES 관리 UI |
| Jaeger | 16686 | 분산 추적 UI |
| Prometheus | 9090 | 메트릭 수집 |
| Grafana | 3001 | 메트릭 대시보드 |

### 3. 백엔드 실행

```bash
./gradlew bootRun
# Windows: gradlew bootRun
```

`spring.profiles.active=local` 로 실행 (application-local.yml 참조)

- MySQL: `root / 0000 @ localhost:3306/blog`
- Redis: `localhost:6379` (인증 없음)
- Elasticsearch: `localhost:9200` (보안 비활성화)

### 4. 프론트엔드 실행

```bash
cd FE
npm install
npm run dev       # http://localhost:3000
```

### 5. 접근 URL 정리

| 서비스 | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/api/v1/swagger-ui/index.html |
| Actuator Health | http://localhost:8081/actuator/health |
| Actuator Prometheus | http://localhost:8081/actuator/prometheus |
| Jaeger UI | http://localhost:16686 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3001 |
| Kibana | http://localhost:5601 |

### 6. 배치 수동 실행 (선택)

```bash
# 특정 날짜 인기글 집계 배치
curl -X POST "http://localhost:8080/api/v1/popular-posts/popular?targetDate=2026-03-31"

# ES Sync Repair 배치 (최근 7일 범위)
curl -X POST "http://localhost:8080/api/v1/test/batch/repair-es?from=2026-03-25T00:00:00"
```

---

## 11. 프론트엔드

### 주요 페이지

| 페이지 | 경로 | 설명 |
|---|---|---|
| 게시글 목록 | `/posts` | 트렌딩·최신 탭, 페이지네이션 |
| 게시글 상세 | `/posts/:id` | 마크다운 렌더링, 댓글, 좋아요 |
| 검색 | `/search` | 검색 전략 선택, 무한 스크롤 |
| 게시글 작성 | `/write` | 마크다운 에디터, 태그 입력 |
| 로그인 | `/login` | JWT 발급 |
| 회원가입 | `/signup` | 이메일/닉네임 |

### 인증 흐름

```
로그인 성공
  → AccessToken: Zustand (메모리)
  → RefreshToken: HttpOnly Cookie (서버 자동 관리)

API 요청
  → Request Interceptor: Authorization 헤더 자동 부착

401 응답
  → Response Interceptor: RefreshToken으로 재발급
  → 새 AccessToken으로 원래 요청 재시도
  → 재발급 실패 시 로그아웃 처리
```

### 검색 전략 선택 UI

```tsx
// SearchPage.tsx
const strategies = [
    { value: "infix",          label: "Infix (LIKE %keyword%)" },
    { value: "prefix",         label: "Prefix (LIKE keyword%)" },
    { value: "fulltext",       label: "Fulltext (Natural Language)" },
    { value: "fulltextBoolean",label: "Fulltext Boolean (+필수 -제외 *와일드)" },
    { value: "es",             label: "Elasticsearch" }
];
```

---

## 데이터베이스 스키마 요약

```
user            — userId, email, password(bcrypt), nickname, role
post            — postId, authorId, title, content(LONGTEXT), postStatus,
                  viewCount, likeCount, syncVersion, createdAt, updatedAt, deletedAt
post_tag        — postId, tagId (N:M 조인 테이블)
tag             — tagId, tagName
post_like       — postLikeId, postId, userId  (UK: postId+userId)
comment         — id, postId, authorId, content(1000), status, createdAt, updatedAt
series          — id, ownerId, name, description  (UK: ownerId+name)
outbox_event    — id, aggregateType, aggregateId, eventType, payload,
                  version, status, retryCount, nextRetryAt, createdAt, processedAt
popular_posts_daily — id, targetDate, postId, rankNo, score  (UK: date+postId, date+rankNo)
```

---

## 향후 개선 방향

- [ ] **Kafka 도입**: Outbox 폴링 → Kafka CDC로 이벤트 처리 방식 전환
- [ ] **검색어 자동완성**: ES `completion` suggester 활용
- [ ] **알림 기능**: 댓글/좋아요 실시간 알림 (SSE 또는 WebSocket)
- [ ] **Redis Cluster**: 단일 Redis → Cluster 전환으로 고가용성 확보
- [ ] **쿼리 튜닝**: Slow Query 분석 및 인덱스 최적화
- [ ] **캐시 레이어**: 게시글 상세 조회에 Cache-Aside 패턴 적용
