package com.example.blog.support;

import com.example.blog.global.security.jwt.JWTUtil;
import com.example.blog.notification.repository.NotificationRepository;
import com.example.blog.comment.repository.CommentRepository;
import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.post.repository.PostLikeRepository;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.search.outbox.repository.OutboxEventRepository;
import com.example.blog.series.repository.SeriesRepository;
import com.example.blog.tag.repository.TagRepository;
import com.example.blog.user.entity.User;
import com.example.blog.user.entity.UserRole;
import com.example.blog.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36")
            .withDatabaseName("blog_test")
            .withUsername("test")
            .withPassword("test");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JWTUtil jwtUtil;

    @Autowired
    protected OutboxEventRepository outboxEventRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @Autowired
    protected CommentRepository commentRepository;

    @Autowired
    protected PostRepository postRepository;

    @Autowired
    protected PostLikeRepository postLikeRepository;

    @Autowired
    protected SeriesRepository seriesRepository;

    @Autowired
    protected TagRepository tagRepository;

    @Autowired
    protected UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        notificationRepository.deleteAll();
        commentRepository.deleteAll();
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        seriesRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User createUser(String email, String nickname, String username) {
        User user = new User();
        user.setEmail(email);
        user.setNickname(nickname);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    protected String createAccessToken(User user) {
        return jwtUtil.createJwt(user.getEmail(), "ROLE_" + user.getRole().name(), 1000L * 60 * 60);
    }

    protected String bearerToken(User user) {
        return "Bearer " + createAccessToken(user);
    }

    protected Post createPost(User author, String title, String content) {
        Post post = new Post();
        post.setAuthor(author);
        post.setTitle(title);
        post.setContent(content);
        post.setPostStatus(PostStatus.PUBLISHED);
        post.setViewCount(0L);
        post.setLikeCount(0L);
        return postRepository.save(post);
    }
}
