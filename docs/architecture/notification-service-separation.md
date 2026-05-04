# Notification Service Separation

## 1. 현재 구조

현재 알림 기능은 모놀리스 내부 `com.example.blog.notification` 패키지에 구현되어 있다.

- `Notification` 엔티티는 특정 사용자에게 귀속되는 `recipientUserId`를 가진다.
- 조회 기능은 사용자별 알림 목록, 안 읽은 알림 수를 제공한다.
- 상태 변경 기능은 단건 읽음 처리, 전체 읽음 처리를 제공한다.
- 권한 검증은 `recipientUserId` 기준으로 수행하며, 다른 사용자의 `notificationId`로는 조회/수정할 수 없다.
- 알림 생성은 도메인 서비스가 `NotificationService`를 직접 호출하지 않고, `outbox_event` 테이블에 이벤트를 적재한 뒤 모놀리스 내부 handler/relay가 처리한다.
- 현재 구현은 in-app notification 범위에 한정되며, WebSocket, SSE, Push, 이메일 발송은 포함하지 않는다.

즉, 현재 단계의 알림은 "사용자별 저장형 알림 도메인"으로 정의되어 있고, 전송 채널보다는 도메인 모델과 접근 제어를 먼저 고정한 상태다.

## 2. 왜 알림이 분리 후보인지

알림은 게시글, 댓글, 인증 같은 핵심 요청 흐름과 성격이 다르기 때문에 MSA 분리 후보가 될 수 있다.

### 2.1 핵심 도메인과 생명주기가 다름

알림은 보통 다른 도메인 이벤트의 결과물로 생성된다. 즉, 게시글 작성이나 댓글 작성이 핵심 업무라면 알림은 그 후속 반응이다. 이런 구조는 핵심 쓰기 모델과 별도의 생명주기를 가지기 쉽다.

### 2.2 비동기 처리와 잘 맞음

알림은 사용자에게 즉시 보여줄 수도 있지만, 본질적으로는 "이벤트를 받아 저장하고 전달하는 작업"에 가깝다. 그래서 동기 API 호출보다 비동기 이벤트 소비 구조와 잘 맞는다.

### 2.3 장애 격리가 필요할 수 있음

알림 채널 장애가 핵심 쓰기 기능 전체를 막아서는 안 된다. 예를 들어 나중에 push/email/SSE 전송이 붙으면 외부 의존성이 늘어나므로, 알림 계층을 분리하면 핵심 도메인 장애 전파를 줄이기 쉽다.

### 2.4 발송 채널이 확장될 수 있음

현재는 in-app notification만 있지만, 향후에는 다음 같은 채널이 추가될 수 있다.

- 웹 실시간 알림
- 모바일 push
- 이메일
- 관리자 시스템 알림

이 시점부터는 알림 저장과 채널별 전송 정책이 별도 관심사가 된다.

### 2.5 조회/읽음 처리 트래픽이 별도로 커질 수 있음

알림 목록 조회, unread count polling, 읽음 처리 요청은 핵심 기능과 다른 트래픽 패턴을 가진다. 사용자 수가 늘면 이 트래픽을 별도로 튜닝하거나 저장소를 분리할 필요가 생길 수 있다.

## 3. 지금 당장 분리하지 않은 이유

현재 단계에서 notification-service를 바로 분리하지 않은 이유는 복잡도 대비 실익이 낮기 때문이다.

- 현재 규모에서는 모놀리스 내부 구현이 더 단순하다.
- Kafka, RabbitMQ, WebSocket, SSE, FCM, email 인프라를 조기에 붙이면 운영 복잡도만 증가할 수 있다.
- 지금 필요한 것은 채널 확장보다 알림 도메인 자체의 경계, 권한 모델, 읽음 처리 규칙을 먼저 안정화하는 것이다.

즉, 지금은 "서비스 분리"보다 "도메인 경계 분리"가 우선이다. 현재 구현은 모놀리스 안에 있지만, 패키지/서비스/API 단위로 경계를 먼저 만들었다는 점이 중요하다.

## 4. 향후 분리 전략

현재 구조는 추후 notification-service로 옮기기 위한 출발점으로 볼 수 있다.

### 4.1 notification-service 생성

모놀리스 내부 `notification` 패키지를 별도 애플리케이션으로 분리한다.

- notification-service
- notification 전용 DB 또는 schema
- 알림 생성/조회/읽음 처리 API

### 4.2 직접 호출 대신 이벤트 발행으로 전환

현재는 다른 도메인이 알림을 만들고 싶다면 `NotificationService`를 직접 호출하는 구조를 사용할 수 있다. 분리 이후에는 이 결합을 줄이기 위해 "알림 생성 요청 이벤트"를 발행하도록 전환한다.

예시:

- 댓글 작성됨
- 프로젝트 활동 생성됨
- 에피소드 발행됨

이런 도메인 이벤트를 알림 생성의 입력으로 바꾼다.

### 4.3 Outbox 테이블로 이벤트 저장

트랜잭션 안정성을 위해 핵심 도메인 서비스는 비즈니스 데이터 저장과 함께 outbox 이벤트를 기록한다.

- 현재 구현에서는 검색 인덱싱과 알림 생성이 같은 `outbox_event` 인프라를 공유한다.
- 검색 이벤트는 DB와 검색 인덱스 사이 eventual consistency를 맞추기 위한 목적이다.
- 알림 이벤트는 핵심 도메인 트랜잭션과 부가 사이드이펙트 생성을 분리하기 위한 목적이다.

즉, 같은 outbox_event 테이블을 사용하더라도 용도는 다르다.

- 검색 Outbox: DB와 검색 인덱스 간 eventual consistency
- 알림 Outbox: 핵심 도메인과 부가 사이드이펙트 분리

현재는 모놀리스 내부 handler/relay가 이 이벤트를 처리한다. 이후 별도 consumer 또는 relay process가 outbox를 읽어 알림 이벤트를 전달하는 구조로 확장할 수 있다.

### 4.4 consumer가 이벤트를 읽어 알림 생성

notification-service 또는 중간 consumer가 이벤트를 수신한 뒤 다음 작업을 수행한다.

- 수신 대상 사용자 결정
- 알림 title/message 구성
- in-app notification 저장
- 필요 시 채널별 발송 작업 enqueue

현재 모놀리스에서는 이 역할을 `NotificationOutboxHandler`가 수행한다. notification-service로 분리할 경우 이 handler 또는 consumer 계층을 별도 서비스로 이동시키는 방향이 자연스럽다.

### 4.5 채널 확장

도메인 저장 구조가 안정화되면 이후 다음 채널을 붙일 수 있다.

- WebSocket
- SSE
- FCM
- email

이 항목들은 현재 구현된 기능이 아니라, notification-service 분리 이후 점진적으로 추가 가능한 확장 경로다.

## 5. API 분리 후 예상 구조

분리 이후에는 대략 다음과 같은 형태를 예상할 수 있다.

### 현재

- `blog-api` 또는 모놀리스 애플리케이션 내부
  - post
  - comment
  - auth
  - notification

### 분리 이후 예시

- `blog-api` 또는 `core-api`
  - 핵심 도메인 API
  - 핵심 쓰기 트랜잭션
  - outbox 이벤트 저장

- `notification-service`
  - 알림 생성 API 또는 이벤트 소비
  - 사용자별 알림 조회
  - unread count
  - 읽음 처리
  - 채널 확장 포인트

- `notification DB` 또는 별도 schema
  - notification table
  - 향후 delivery/status/log table 확장 가능

- `event/outbox`
  - 현재는 모놀리스 내부 `outbox_event`
  - 초기에는 DB outbox
  - broker는 추후 선택

메시지 브로커는 향후 트래픽과 운영 요구에 따라 Kafka, RabbitMQ 등으로 선택할 수 있지만, 현재 단계에서 이를 전제해 구현한 것은 아니다.

## 6. 분리 가능성을 뒷받침하는 현재 설계 포인트

현재 모놀리스 구현에도 이미 분리 친화적인 요소가 있다.

- 알림 기능이 별도 패키지로 분리되어 있다.
- `recipientUserId` 기준 권한 규칙이 명확하다.
- 조회/읽음 처리 API가 알림 도메인 내부로 모여 있다.
- 엔티티를 직접 노출하지 않고 DTO를 통해 응답한다.
- 알림 생성은 `outbox_event`를 거쳐 handler가 수행하므로, 핵심 도메인과 부가 사이드이펙트가 직접 결합되지 않는다.
- 검색 인덱싱과 알림 생성이 같은 outbox 인프라를 공유하지만, handler 레벨에서 책임이 분리되어 있다.

즉, 아직 분산 시스템은 아니지만 "경계가 있는 모놀리스" 형태로 준비된 상태라고 설명할 수 있다.

## 7. 포트폴리오에서 설명할 수 있는 핵심 문장

다음 문장은 현재 구현 수준을 과장하지 않으면서도 설계 의도를 설명하는 데 적합하다.

> 처음부터 MSA를 적용하지 않고, 분리 가능성이 높은 알림 도메인을 모듈 경계로 먼저 격리했다.

> 이후 비동기 이벤트 기반 구조와 Outbox 패턴을 통해 notification-service로 확장할 수 있도록 설계했다.

보조 설명으로는 다음 정도가 적절하다.

- 현재는 모놀리스 내부 in-app notification만 구현했다.
- 실시간 전송 채널과 메시지 브로커는 아직 도입하지 않았다.
- 대신 사용자별 조회, unread count, 읽음 처리, 권한 검증을 먼저 고정했다.
- 알림 생성은 모놀리스 내부 handler/relay가 outbox 이벤트를 소비하는 방식으로 구현했다.
- 이후 요구사항과 트래픽이 커지면 알림만 별도 서비스로 분리할 수 있다.

## 8. 현재 구현과 향후 확장의 구분

### 현재 구현 완료

- 모놀리스 내부 notification 도메인
- 사용자별 알림 저장
- 사용자별 목록 조회
- 사용자별 unread count 조회
- 단건 읽음 처리
- 전체 읽음 처리
- recipientUserId 기반 권한 검증
- outbox_event 기반 알림 생성 handler/relay

### 현재 미구현, 향후 확장 가능

- 별도 notification-service 분리
- 외부 broker 기반 event relay / consumer
- WebSocket 실시간 알림
- SSE 스트리밍
- FCM push
- 이메일 발송
- Kafka / RabbitMQ 기반 메시지 브로커 도입

이 구분을 명확히 유지해야 포트폴리오나 문서에서 구현 범위를 과장하지 않을 수 있다.
