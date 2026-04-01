package com.example.blog.search.service;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.search.batch.dto.RepairResult;
import com.example.blog.search.infra.es.document.PostSearchDocument;
import com.example.blog.search.repository.PostSearchRepository;
import com.example.blog.user.entity.User;
import com.example.blog.user.entity.UserRole;
import com.example.blog.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class SearchIndexRepairIntegrationTest {

    @Container
    static MySQLContainer mysql =
            new MySQLContainer("mysql:8.0.36")
                    .withDatabaseName("blog_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.2.1")
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
        registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());
    }

    @Autowired
    private SearchIndexRepairService searchIndexRepairService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostSearchRepository postSearchRepository;

    @Autowired
    private UserRepository userRepository;

    private User author;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("test@example.com");
        user.setNickname("tester");
        user.setUsername("tester");
        user.setPassword("encoded-password");
        user.setRole(UserRole.USER);
        author = userRepository.save(user);
    }

    @Test
    void repair_reindexes_missing_document() {
        Post post = createPublishedPost("재색인 대상", "본문");

        RepairResult result = searchIndexRepairService.repair(LocalDateTime.now().minusDays(7));

        PostSearchDocument repaired = postSearchRepository.findByPostId(post.getPostId()).orElse(null);

        assertThat(repaired).isNotNull();
        assertThat(repaired.getTitle()).isEqualTo("재색인 대상");

        // getter 이름은 네 DTO에 맞춰 사용
        assertThat(result.getReindexed()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("MySQL에서 삭제된 게시글은 repair 시 ES stale 문서가 제거된다")
    void repair_deletes_stale_document_for_deleted_post() {
        Post post = createPublishedPost("삭제 대상", "본문");

        PostSearchDocument staleDoc = createDocument(post, "삭제 대상", "본문");
        postSearchRepository.upsertFullDocument(staleDoc);

        post.softDelete();
        postRepository.save(post);

        RepairResult result = searchIndexRepairService.repair(LocalDateTime.now().minusDays(1));

        assertThat(postSearchRepository.findByPostId(post.getPostId())).isEmpty();
        assertThat(result.getDeleted()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("MySQL과 ES 문서가 다르면 repair 시 최신 값으로 덮어쓴다")
    void repair_updates_outdated_document() {
        Post post = createPublishedPost("최신 제목", "최신 본문");

        PostSearchDocument oldDoc = createDocument(post, "오래된 제목", "오래된 본문");
        postSearchRepository.upsertFullDocument(oldDoc);

        RepairResult result = searchIndexRepairService.repair(LocalDateTime.now().minusDays(1));

        PostSearchDocument repaired = postSearchRepository.findByPostId(post.getPostId()).orElseThrow();

        assertThat(repaired.getTitle()).isEqualTo("최신 제목");
        assertThat(result.getReindexed()).isGreaterThanOrEqualTo(1);
    }

    private Post createPublishedPost(String title, String content) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setAuthor(author);
        post.setPostStatus(PostStatus.PUBLISHED);
        post.setViewCount(0L);
        post.setLikeCount(0L);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    private PostSearchDocument createDocument(Post post, String title, String contentPreview) {
        PostSearchDocument doc = new PostSearchDocument();
        doc.setPostId(post.getPostId());
        doc.setTitle(title);
        doc.setContent(contentPreview);
        return doc;
    }
}