-- =========================
-- USERS
-- =========================
INSERT INTO user (user_id, username, password, email, nickname, role, created_at, updated_at, deleted_at)
VALUES
    (1, 'admin', '{noop}admin1234', 'admin@test.com', '관리자', 'ADMIN', NOW(), NOW(), NULL),
    (2, 'user1', '{noop}pass1234', 'user1@test.com', '유저1', 'USER', NOW(), NOW(), NULL),
    (3, 'user2', '{noop}pass1234', 'user2@test.com', '유저2', 'USER', NOW(), NOW(), NULL);

-- auto_increment 다음 값 맞추기(선택)
ALTER TABLE user AUTO_INCREMENT = 4;


-- =========================
-- TAGS
-- =========================
INSERT INTO tags (tag_id, tag_name)
VALUES
    (1, 'JPA'),
    (2, 'Hibernate'),
    (3, 'MySQL'),
    (4, 'SpringBoot'),
    (5, 'Troubleshooting');

ALTER TABLE tags AUTO_INCREMENT = 6;


-- =========================
-- POSTS
-- =========================
INSERT INTO post (
    post_id,
    author_id,
    title,
    content,
    post_status,
    view_count,
    like_count,
    created_at,
    updated_at,
    deleted_at
)
VALUES
    (1, 2, 'JPA 연관관계 기초', '내용: ManyToOne / OneToMany 정리', 'PUBLISHED', 10, 2, NOW(), NOW(), NULL),
    (2, 2, 'Java 컬렉션 정리', '내용: List/Set/Map 차이', 'PUBLISHED', 3, 0, NOW(), NOW(), NULL),
    (3, 3, 'MySQL 인덱스 기초', '내용: B-Tree 인덱스와 쿼리 플랜', 'DRAFT', 0, 0, NOW(), NOW(), NULL);

ALTER TABLE post AUTO_INCREMENT = 4;

SET SESSION cte_max_recursion_depth = 100000;

-- 2) 대량 더미 데이터 삽입 (post_id는 AUTO_INCREMENT로 자동 생성)
INSERT INTO post (
    author_id,
    title,
    content,
    post_status,
    view_count,
    like_count,
    created_at,
    updated_at,
    deleted_at
)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 100000
)
SELECT
    CASE WHEN n % 5 = 0 THEN 3 ELSE 2 END AS author_id,             -- author_id: 2 또는 3만 사용
    CONCAT('더미 글 #', n) AS title,
    CONCAT('내용: 더미 데이터 ', n, ' - 인덱스/부하 테스트용') AS content,
    CASE WHEN n % 10 = 0 THEN 'DRAFT' ELSE 'PUBLISHED' END AS post_status, -- 90% 발행, 10% 초안
    FLOOR(RAND(n) * 200000) AS view_count,                           -- 조회수 랜덤
    FLOOR(RAND(n * 7) * 5000) AS like_count,                         -- 좋아요 랜덤
    NOW() - INTERVAL (n % 180) DAY - INTERVAL (n % 86400) SECOND AS created_at,
    NOW() - INTERVAL (n % 180) DAY AS updated_at,
    NULL AS deleted_at
FROM seq;

-- 3) 통계 갱신(옵티마이저가 플랜 잘 잡게)
ANALYZE TABLE post;


-- =========================
-- POST_TAG (N:M via join table)
-- unique: (post_id, tag_id)
-- =========================
INSERT INTO post_tag (id, post_id, tag_id)
VALUES
    (1, 1, 1), -- post 1 - JPA
    (2, 1, 2), -- post 1 - Hibernate
    (3, 1, 4), -- post 1 - SpringBoot
    (4, 2, 5), -- post 2 - Troubleshooting
    (5, 3, 3); -- post 3 - MySQL

ALTER TABLE post_tag AUTO_INCREMENT = 6;


-- =========================
-- POST_LIKE
-- =========================
INSERT INTO post_like (post_like_id, post_id, user_id, created_at)
VALUES
    (1, 1, 3, NOW()),  -- user2 likes post1
    (2, 1, 2, NOW());  -- user1 likes post1 (본인 글 좋아요 허용 가정)

ALTER TABLE post_like AUTO_INCREMENT = 3;


-- =========================
-- POST_READ_HISTORY
-- composite PK: (user_id, post_id) via @EmbeddedId
-- =========================
INSERT INTO post_read_history (user_id, post_id, last_read_at, read_count)
VALUES
    (2, 1, NOW(), 3),  -- user1 read post1 3 times
    (3, 1, NOW(), 1),  -- user2 read post1 1 time
    (3, 2, NOW(), 2);  -- user2 read post2 2 times
