-- =========================
-- 0) (선택) 초기화
-- FK 걸려있으면 순서대로 지워야 합니다.
-- =========================
-- SET FOREIGN_KEY_CHECKS = 0;
-- DELETE FROM post_read_history;
-- DELETE FROM post_like;
-- DELETE FROM post_tag;
-- DELETE FROM post;
-- DELETE FROM tags;
-- DELETE FROM categories;
-- DELETE FROM user;
-- SET FOREIGN_KEY_CHECKS = 1;

-- =========================
-- 1) Categories
-- table: categories
-- columns: categories_id, categories_name
-- =========================
INSERT INTO categories (categories_name) VALUES
                                                            ('BACKEND'),
                                                            ('FRONTEND'),
                                                            ('DEVOPS'),
                                                            ('ETC');

-- =========================
-- 2) Users
-- table: user
-- columns (snake_case): user_id, username, password, email, nickname, role, created_at, updated_at, deleted_at
-- NOTE: password는 실제 로그인 테스트를 하려면 인코딩(BCrypt 등)된 값이어야 합니다.
-- =========================
INSERT INTO user (username, password, email, nickname, role, created_at, updated_at, deleted_at) VALUES
                                                                                                              ('donghun', 'dummy-password', 'donghun@example.com', '동훈', 'USER',  '2026-01-01 10:00:00', '2026-01-01 10:00:00', NULL),
                                                                                                              ('admin',   'dummy-password', 'admin@example.com',   '관리자', 'ADMIN','2026-01-01 10:05:00', '2026-01-01 10:05:00', NULL),
                                                                                                              ('jane',    'dummy-password', 'jane@example.com',    '제인', 'USER',  '2026-01-02 09:00:00', '2026-01-02 09:00:00', NULL);

-- =========================
-- 3) Tags
-- table: tags
-- columns: tag_id, tag_name
-- =========================
INSERT INTO tags (tag_id, tag_name) VALUES
                                        (1, 'spring'),
                                        (2, 'blog'),
                                        (3, 'jpa'),
                                        (4, 'react'),
                                        (5, 'security');

-- =========================
-- 4) Posts
-- table: post
-- 예상 columns:
--   post_id
--   author_user_id                (Post.author)
--   categories_id_categories_id    (Post.categoriesId)  <-- 필드명이 categoriesId라 이렇게 나올 가능성 큼
--   title, content, post_status, view_count, like_count, created_at, updated_at, deleted_at
-- =========================
INSERT INTO post (
    author_id,
    category_id,
    title,
    content,
    post_status,
    view_count,
    like_count,
    created_at,
    updated_at,
    deleted_at
) VALUES
      (1, 1, '스프링 블로그 만들기',
       '마크다운/HTML 포함한 본문 내용...\n\n- Spring Boot\n- JPA\n- Pagination\n',
       'PUBLISHED', 123, 5, '2026-01-03 10:00:00', '2026-01-03 11:00:00', NULL),

      (1, 1, 'JPA 동적 검색 정리',
       'Specification 없이 @Query로도 동적 조건 검색을 구현할 수 있습니다...',
       'PUBLISHED', 45, 2, '2026-01-03 12:30:00', '2026-01-03 12:45:00', NULL),

      (3, 2, '리액트 홈 화면 구성',
       '탭 UI, skeleton loading, 카드 리스트를 구성하는 방법...',
       'PUBLISHED', 10, 0, '2026-01-03 14:10:00', '2026-01-03 14:10:00', NULL),

      (1, 1, '임시글: 초안',
       '아직 공개하지 않은 초안입니다.',
       'DRAFT', 0, 0, '2026-01-03 15:00:00', '2026-01-03 15:00:00', NULL);


-- =========================
-- 5) PostTag (N:M 중간테이블)
-- table: post_tag
-- columns: id, post_id, tag_id
-- unique: (post_id, tag_id)
-- =========================
INSERT INTO post_tag (post_id, tag_id) VALUES
                                               (1, 1), -- post 10 - spring
                                               (1, 2), -- post 10 - blog
                                               (1, 3), -- post 11 - jpa
                                               (2, 1), -- post 11 - spring
                                               (2, 4), -- post 12 - react
                                               (3, 5); -- post 10 - security

-- =========================
-- 6) PostLike
-- table: post_like
-- 예상 columns:
--   post_like_id
--   post_post_id   (PostLike.post)
--   user_user_id   (PostLike.user)
--   created_at
-- =========================
INSERT INTO post_like (post_id, user_id, created_at) VALUES
                                                                       (1, 2, '2026-01-03 10:05:00'),
                                                                       (3, 3, '2026-01-03 10:06:00'),
                                                                       (4, 3, '2026-01-03 12:50:00');


-- =========================
-- 7) PostReadHistory
-- table: post_read_history
-- columns: user_id, post_id, last_read_at, read_count
-- (EmbeddedId라 user_id+post_id가 PK)
-- =========================
INSERT INTO post_read_history (user_id, post_id, last_read_at, read_count) VALUES
                                                                               (1, 1, '2026-01-03 11:07:00', 3),
                                                                               (2, 1, '2026-01-03 11:08:00', 1),
                                                                               (3, 2, '2026-01-03 12:55:00', 2);
