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
WITH
    digits AS (
        SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
        UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
    ),
    seq AS (
        -- 1 ~ 1,000,000 생성 가능 (필요하면 WHERE에서 자르기)
        SELECT
            (a.n + b.n*10 + c.n*100 + d.n*1000 + e.n*10000 + f.n*100000) + 1 AS n
        FROM digits a
                 CROSS JOIN digits b
                 CROSS JOIN digits c
                 CROSS JOIN digits d
                 CROSS JOIN digits e
                 CROSS JOIN digits f
    )
SELECT
    CASE WHEN n % 5 = 0 THEN 3 ELSE 2 END AS author_id,

    /* -----------------------
       title: 검색 분포 설계
       - 0~19%  : prefix(앞부분 일치)로 잘 걸리게
       - 20~49% : infix(중간 포함)로만 걸리게
       - 50~59% : 오타/띄어쓰기 변형(약한 오타 내성 테스트용)
       - 60~99% : 잡음(키워드 없는 글)
       ----------------------- */
    CONCAT(
        /* 1) 핫 키워드(반복) - prefix/infix 실험 유지 */
            CASE
                WHEN n % 100 < 20 THEN CONCAT(  -- 20%: prefix
                        ELT((CRC32(CONCAT('k1', n)) % 23) + 1,
                            'select','spring','redis','elasticsearch','mysql','jwt','docker','ecs','rds','locust','index','like',
                            'kafka','k8s','nginx','jpa','hibernate','query','transaction','lock','cache','search','autocomplete'
                        ),
                        ' '
                                       )
                WHEN n % 100 >= 20 AND n % 100 < 50 THEN '' -- 30%: infix는 뒤에서 삽입
    WHEN n % 100 >= 50 AND n % 100 < 60 THEN CONCAT( -- 10%: 오타/변형
      ELT((CRC32(CONCAT('typo', n)) % 12) + 1,
        'selcet','sprng','redsi','elasticserach','myqsl','jwtt','docer','ecss','indx','auto complete','serach','autocomplte'
      ),
      ' '
    )
    ELSE ''  -- 40%: 잡음
  END,

        /* 2) 랜덤 “주제(명사)” */
            ELT((CRC32(CONCAT('topic', n)) % 40) + 1,
                '검색','정렬','페이지네이션','무한스크롤','인덱스','쿼리플랜','슬로우쿼리','트랜잭션','락','동시성','캐시','역색인',
                '자동완성','오타내성','토큰화','분석기','스코어링','필터','집계','샤딩','파티셔닝','커넥션풀','타임아웃','리트라이',
                '배포','롤링업데이트','모니터링','로깅','메트릭','알람','부하테스트','레이트리밋','서킷브레이커','보안','인증','인가',
                '리팩터링','아키텍처','장애대응','성능개선'
            ),

        /* 3) 랜덤 “행동(동사/구)” */
            ' ',
            ELT((CRC32(CONCAT('verb', n)) % 29) + 1,
                '정리','분석','재현','최적화','비교','개선','설계','적용','검증','튜닝','측정','관측',
                '문제추적','원인분석','실험','대안검토','결과정리','로그분석','지표수집','병목제거',
                '도입','제거','전환','리팩터링','회고','가이드','체크리스트','트러블슈팅','포스트모템'
            ),

        /* 4) infix 구간(30%)은 여기서 키워드를 “중간에” 박아 넣기 */
            CASE
                WHEN n % 100 >= 20 AND n % 100 < 50 THEN CONCAT(
      ' - ',
      ELT((CRC32(CONCAT('k2', n)) % 23) + 1,
        'select','spring','redis','elasticsearch','mysql','jwt','docker','ecs','rds','locust','index','like',
        'kafka','k8s','nginx','jpa','hibernate','query','transaction','lock','cache','search','autocomplete'
      )
    )
    ELSE ''
  END,

        /* 5) 랜덤 “수식어/케이스” (다양성 + 현실감) */
            ' ',
            ELT((CRC32(CONCAT('case', n)) % 17) + 1,
                '[p95]','[p99]','[EXPLAIN]','[slow-query]','[offset]','[cursor]','[benchmark]','[A/B]','[regression]',
                '(핵심)','(실전)','(주의)','(요약)','(실험)','(결론)','(비교)','(대안)'
            ),

        /* 6) 고유성 보장용 번호 */
            ' #', LPAD(n, 7, '0')
    ) AS title
        ,

    /* content: 키워드가 여러 번 나오게(검색 느낌) */
    CONCAT(
            '요약: ', ELT(((n * 3) % 6) + 1, '성능', '검색', 'DB', '캐시', '인프라', '보안'), '\n',
            '태그: ',
            ELT((n % 12) + 1, 'spring', 'jpa', 'redis', 'elasticsearch', 'mysql', 'docker', 'ecs', 'rds', 'jwt', 'locust', 'index', 'like'),
            ', ',
            ELT(((n * 11) % 12) + 1, 'spring', 'jpa', 'redis', 'elasticsearch', 'mysql', 'docker', 'ecs', 'rds', 'jwt', 'locust', 'index', 'like'),
            ', ',
            ELT(((n * 17) % 12) + 1, 'spring', 'jpa', 'redis', 'elasticsearch', 'mysql', 'docker', 'ecs', 'rds', 'jwt', 'locust', 'index', 'like'),
            '\n',
            '본문: 더미 데이터 ', n, ' - 검색 품질/성능 비교용. ',
            ELT(((n * 13) % 8) + 1,
            'LIKE ''%keyword%''는 rows examined가 커지는지 확인한다.',
            'prefix 검색은 인덱스가 타는지 EXPLAIN으로 본다.',
            'offset이 깊어질수록 tail latency가 오르는지 관측한다.',
            'search_as_you_type로 자동완성(prefix/infix)을 비교한다.',
            'JPA 페치 전략으로 N+1이 생기지 않게 한다.',
            'Locust로 p95/p99를 기록하고 그래프로 남긴다.',
            'ECS 롤링 배포에서 이미지 태그 기반 배포를 재현한다.',
            'Redis 캐시/랭킹과 DB 원본 저장을 분리한다.'
        )
    ) AS content,

    CASE WHEN n % 10 = 0 THEN 'DRAFT' ELSE 'PUBLISHED' END AS post_status,

    (CRC32(CONCAT('v', n)) % 200000) AS view_count,
    (CRC32(CONCAT('l', n)) % 5000)   AS like_count,

    NOW() - INTERVAL (n % 180) DAY - INTERVAL (n % 86400) SECOND AS created_at,
    NOW() - INTERVAL (n % 180) DAY AS updated_at,
    NULL AS deleted_at
FROM seq
WHERE n <= 500000;


-- 3) 통계 갱신
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
