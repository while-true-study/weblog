package com.example.blog.loadtest;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.user.entity.User;
import com.example.blog.user.entity.UserRole;
import com.example.blog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class LoadtestDataBootstrap implements ApplicationRunner {

    public static final String AUTHOR_EMAIL = "loadtest.author@example.com";
    public static final String COMMENTER_EMAIL = "loadtest.commenter@example.com";
    public static final String DEFAULT_PASSWORD = "password123!";
    public static final String POST_TITLE_PREFIX = "[loadtest] post ";
    private static final int POST_COUNT = 10;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User author = upsertUser(AUTHOR_EMAIL, "postAuthor", "postAuthor");
        upsertUser(COMMENTER_EMAIL, "commenter", "commenter");

        for (int i = 1; i <= POST_COUNT; i++) {
            String title = POST_TITLE_PREFIX + String.format("%02d", i);
            upsertPost(author, title, i);
        }

        log.info("Loadtest bootstrap ready. authorEmail={}, commenterEmail={}, postCount={}",
                AUTHOR_EMAIL, COMMENTER_EMAIL, POST_COUNT);
    }

    private User upsertUser(String email, String nickname, String username) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);

        user.setEmail(email);
        user.setNickname(nickname);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRole(UserRole.USER);

        LocalDateTime now = LocalDateTime.now();
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);

        return userRepository.save(user);
    }

    private void upsertPost(User author, String title, int index) {
        Post post = postRepository.findByAuthorUserIdAndTitle(author.getUserId(), title).orElseGet(Post::new);

        post.setAuthor(author);
        post.setTitle(title);
        post.setContent("""
                # Loadtest Post %d

                This post is reserved for k6 load tests.
                """.formatted(index));
        post.setPostStatus(PostStatus.PUBLISHED);
        post.setViewCount(post.getViewCount() == null ? 0L : post.getViewCount());
        post.setLikeCount(post.getLikeCount() == null ? 0L : post.getLikeCount());

        LocalDateTime now = LocalDateTime.now();
        if (post.getCreatedAt() == null) {
            post.setCreatedAt(now);
        }
        post.setUpdatedAt(now);

        postRepository.save(post);
    }
}
