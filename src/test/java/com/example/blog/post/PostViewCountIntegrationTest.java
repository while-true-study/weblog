package com.example.blog.post;

import com.example.blog.post.entity.Post;
import com.example.blog.post.presentation.dto.response.PostDetailDto;
import com.example.blog.post.service.PostService;
import com.example.blog.post.service.RedisPostRankingCache;
import com.example.blog.post.service.RedisPostViewDedupService;
import com.example.blog.popular.service.PopularEventService;
import com.example.blog.support.IntegrationTestSupport;
import com.example.blog.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostViewCountIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private PostService postService;

    @MockitoBean
    private RedisPostViewDedupService redisPostViewDedupService;

    @MockitoBean
    private RedisPostRankingCache redisPostRankingCache;

    @MockitoBean
    private PopularEventService popularEventService;

    @Test
    @DisplayName("첫 조회는 viewCount를 1 증가시키고 ranking/popular 반영을 수행한다")
    void firstView_increasesViewCountAndReflectsRanking() {
        User author = createUser("view-author-1@test.com", "author1", "author1");
        Post post = createPost(author, "first title", "first content");

        when(redisPostViewDedupService.shouldIncrease(post.getPostId(), "viewer-a")).thenReturn(true);
        doNothing().when(redisPostRankingCache).increaseViewScore(post.getPostId());
        doNothing().when(popularEventService).reflectView(post.getPostId());

        PostDetailDto detail = postService.getPost(post.getPostId(), "viewer-a");

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(detail.viewCount()).isEqualTo(1L);
        assertThat(updated.getViewCount()).isEqualTo(1L);
        verify(redisPostViewDedupService).shouldIncrease(post.getPostId(), "viewer-a");
        verify(redisPostRankingCache).increaseViewScore(post.getPostId());
        verify(popularEventService).reflectView(post.getPostId());
    }

    @Test
    @DisplayName("같은 viewer가 24시간 내 다시 조회하면 viewCount는 더 이상 증가하지 않는다")
    void sameViewerWithinDedupWindow_doesNotIncreaseViewCountTwice() {
        User author = createUser("view-author-2@test.com", "author2", "author2");
        Post post = createPost(author, "same viewer title", "same viewer content");

        when(redisPostViewDedupService.shouldIncrease(post.getPostId(), "viewer-a"))
                .thenReturn(true)
                .thenReturn(false);

        postService.getPost(post.getPostId(), "viewer-a");
        PostDetailDto second = postService.getPost(post.getPostId(), "viewer-a");

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(second.viewCount()).isEqualTo(1L);
        assertThat(updated.getViewCount()).isEqualTo(1L);
        verify(redisPostViewDedupService, times(2)).shouldIncrease(post.getPostId(), "viewer-a");
        verify(redisPostRankingCache, times(1)).increaseViewScore(post.getPostId());
        verify(popularEventService, times(1)).reflectView(post.getPostId());
    }

    @Test
    @DisplayName("다른 viewer가 조회하면 viewCount가 다시 증가한다")
    void differentViewer_increasesViewCountAgain() {
        User author = createUser("view-author-3@test.com", "author3", "author3");
        Post post = createPost(author, "different viewer title", "different viewer content");

        when(redisPostViewDedupService.shouldIncrease(post.getPostId(), "viewer-a")).thenReturn(true);
        when(redisPostViewDedupService.shouldIncrease(post.getPostId(), "viewer-b")).thenReturn(true);

        postService.getPost(post.getPostId(), "viewer-a");
        PostDetailDto second = postService.getPost(post.getPostId(), "viewer-b");

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(second.viewCount()).isEqualTo(2L);
        assertThat(updated.getViewCount()).isEqualTo(2L);
        verify(redisPostRankingCache, times(2)).increaseViewScore(post.getPostId());
        verify(popularEventService, times(2)).reflectView(post.getPostId());
    }

    @Test
    @DisplayName("Redis dedup 장애가 발생해도 게시글 상세 조회는 성공하고 조회수 반영은 생략한다")
    void redisDedupFailure_doesNotBreakPostDetailRead() {
        User author = createUser("view-author-4@test.com", "author4", "author4");
        Post post = createPost(author, "dedup failure title", "dedup failure content");

        doThrow(new RuntimeException("redis dedup down"))
                .when(redisPostViewDedupService)
                .shouldIncrease(post.getPostId(), "viewer-a");

        PostDetailDto detail = postService.getPost(post.getPostId(), "viewer-a");

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(detail.id()).isEqualTo(post.getPostId());
        assertThat(detail.viewCount()).isZero();
        assertThat(updated.getViewCount()).isZero();
        verify(redisPostRankingCache, never()).increaseViewScore(post.getPostId());
        verify(popularEventService, never()).reflectView(post.getPostId());
    }

    @Test
    @DisplayName("ranking 또는 popular 반영이 실패해도 상세 조회는 성공하고 증가한 viewCount는 유지된다")
    void rankingSideEffectFailure_doesNotBreakPostDetailRead() {
        User author = createUser("view-author-5@test.com", "author5", "author5");
        Post post = createPost(author, "ranking failure title", "ranking failure content");

        when(redisPostViewDedupService.shouldIncrease(post.getPostId(), "viewer-a")).thenReturn(true);
        doThrow(new RuntimeException("ranking redis down"))
                .when(redisPostRankingCache)
                .increaseViewScore(post.getPostId());

        assertThatCode(() -> postService.getPost(post.getPostId(), "viewer-a"))
                .doesNotThrowAnyException();

        Post updated = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(updated.getViewCount()).isEqualTo(1L);
        verify(redisPostRankingCache).increaseViewScore(post.getPostId());
        verify(popularEventService, never()).reflectView(post.getPostId());
    }
}
