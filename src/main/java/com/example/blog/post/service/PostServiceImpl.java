package com.example.blog.post.service;

import com.example.blog.global.observability.TraceHelper;
import com.example.blog.popular.service.PopularEventService;
import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.post.presentation.dto.request.PostPublishedDto;
import com.example.blog.post.presentation.dto.request.PostUpdateRequest;
import com.example.blog.post.presentation.dto.response.*;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.post.repository.spec.PostSpecs;
import com.example.blog.search.outbox.service.PostOutboxService;
import com.example.blog.series.entity.Series;
import com.example.blog.series.repository.SeriesRepository;
import com.example.blog.tag.entity.Tag;
import com.example.blog.tag.repository.TagRepository;
import com.example.blog.user.entity.User;
import com.example.blog.user.repository.UserRepository;
import com.example.blog.global.exception.BlogException;
import com.example.blog.global.exception.ErrorCode;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final SeriesRepository seriesRepository;
    private final PostOutboxService postOutboxService;
    private final RedisPostViewDedupService redisPostViewDedupService;
    private final RedisPostRankingCache redisPostRankingCache;
    private final PopularEventService popularEventService;
    private final ObservationRegistry observationRegistry;
    private final TraceHelper traceHelper;

    //    private final DbShardViewCounter viewCountService; // 샤딩 버전
    //    private final ApplicationEventPublisher eventPublisher; // event 퍼블리셔

    @Transactional(readOnly = true)
    @Override
    public Slice<PostListItemDto> getPosts(PostSearchCond cond, Pageable pageable) {
        return traceHelper.trace("post.list.read", () -> {
            Specification<Post> spec =
                    PostSpecs.hasTag(cond.tag())
                            .and(PostSpecs.status(PostStatus.PUBLISHED));

            return postRepository.findBy(spec, query ->
                    query.sortBy(pageable.getSort())
                            .slice(pageable)
            ).map(PostListItemDto::from);
        });
    }

    @Override
    @Transactional
    public PostDetailDto getPost(Long id, String viewerId) {
        return traceHelper.trace("post.detail.read", () -> {
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

            if (post.getPostStatus() == PostStatus.DELETED) {
                throw new BlogException(ErrorCode.POST_NOT_FOUND);
            }

            boolean shouldIncrease = redisPostViewDedupService.shouldIncrease(id, viewerId);

            if (shouldIncrease) {
                post.increaseViewCount();
                post.increaseSyncVersion();
                postOutboxService.createUpdatedEvent(post);
                redisPostRankingCache.increaseViewScore(id);
                popularEventService.reflectView(id);
            }

            return PostDetailDto.from(post, post.getViewCount());
        });
    }

    @Transactional
    @Override
    public PostCreateResponse createPost(PostPublishedDto postPublishedDto, String username) {
        return traceHelper.trace("post.create", () -> {
            User author = traceHelper.trace("post.create.find-author", () ->
                    userRepository.findByEmail(username)
                            .orElseThrow(() -> new BlogException(ErrorCode.USER_NOT_FOUND))
            );

            Post post = new Post();
            post.setTitle(postPublishedDto.title());
            post.setAuthor(author);
            post.setContent(postPublishedDto.content());
            post.setPostStatus(PostStatus.PUBLISHED);
            post.setViewCount(0L);
            post.setLikeCount(0L);
            post.setCreatedAt(LocalDateTime.now());
            post.setUpdatedAt(LocalDateTime.now());

            if (postPublishedDto.seriesId() != null) {
                Series series = traceHelper.trace("post.create.find-series", () ->
                        seriesRepository.findByIdAndOwner_UserId(postPublishedDto.seriesId(), author.getUserId())
                                .orElseThrow(() -> new BlogException(ErrorCode.SERIES_NOT_FOUND))
                );
                post.setSeries(series);
            }

            traceHelper.trace("post.create.attach-tags", () -> {
                post.clearTags();

                if (postPublishedDto.tags() != null && !postPublishedDto.tags().isEmpty()) {
                    postPublishedDto.tags().stream()
                            .filter(t -> t != null && !t.isBlank())
                            .map(String::trim)
                            .distinct()
                            .forEach(tagName -> {
                                Tag tag = tagRepository.findByTagName(tagName)
                                        .orElseGet(() -> tagRepository.save(Tag.of(tagName)));
                                post.addTag(tag);
                            });
                }
            });

            Post saved = traceHelper.trace("post.create.save", () -> postRepository.save(post));

            traceHelper.trace("post.create.outbox-created", () ->
                    postOutboxService.createCreatedEvent(saved)
            );

            return PostCreateResponse.from(saved);
        });
    }

    @Transactional
    @Override
    public PostUpdateResponse updatePost(Long postId, PostUpdateRequest req, String email) {
        return traceHelper.trace("post.update", () -> {
            Post post = traceHelper.trace("post.update.load", () ->
                    postRepository.findById(postId)
                            .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND))
            );

            if (!post.getAuthor().getEmail().equals(email)) {
                throw new BlogException(ErrorCode.POST_FORBIDDEN);
            }

            traceHelper.trace("post.update.apply-changes", () -> {
                post.setTitle(req.getTitle());
                post.setContent(req.getContent());
                post.setUpdatedAt(LocalDateTime.now());
                post.increaseSyncVersion();
            });

            traceHelper.trace("post.update.outbox-created", () ->
                    postOutboxService.createUpdatedEvent(post)
            );

            return new PostUpdateResponse(
                    post.getPostId(),
                    post.getTitle(),
                    post.getSyncVersion(),
                    post.getUpdatedAt()
            );
        });
    }

    @Transactional
    @Override
    public void deletePost(Long postId, String email) {
        traceHelper.trace("post.delete", () -> {
            Post post = traceHelper.trace("post.delete.load", () ->
                    postRepository.findById(postId)
                            .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND))
            );

            validateAuthor(post, email);

            if (post.getDeletedAt() != null || post.getPostStatus() == PostStatus.DELETED) {
                throw new BlogException(ErrorCode.POST_ALREADY_DELETED);
            }

            traceHelper.trace("post.delete.soft-delete", post::softDelete);

            traceHelper.trace("post.delete.outbox-deleted", () ->
                    postOutboxService.createDeletedEvent(post)
            );
        });
    }

    private void validateAuthor(Post post, String email) {
        User author = post.getAuthor();
        if (author == null || author.getEmail() == null || !author.getEmail().equals(email)) {
            throw new BlogException(ErrorCode.POST_FORBIDDEN);
        }
    }
}
