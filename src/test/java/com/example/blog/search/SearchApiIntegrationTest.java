package com.example.blog.search;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.search.infra.es.EsSearchSliceResult;
import com.example.blog.search.infra.es.PostSearchEsRepository;
import com.example.blog.search.infra.es.document.PostSearchDocument;
import com.example.blog.support.IntegrationTestSupport;
import com.example.blog.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PostSearchEsRepository postSearchEsRepository;

    @BeforeEach
    void ensureFulltextIndex() {
        Long indexCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'post'
                  AND index_name = 'ft_post_title'
                """,
                Long.class
        );

        if (indexCount == null || indexCount == 0L) {
            jdbcTemplate.execute("ALTER TABLE post ADD FULLTEXT INDEX ft_post_title (title)");
        }

        when(postSearchEsRepository.searchTitle(anyString(), anyInt(), anyInt()))
                .thenReturn(new EsSearchSliceResult(List.of(), false));
    }

    @Test
    @DisplayName("검색 API는 미인증 상태에서도 공개 접근 가능하다")
    void searchApis_areAccessibleWithoutAuthentication() throws Exception {
        List<String> mysqlPaths = List.of(
                "/api/v1/search/posts",
                "/api/v1/search/posts/infix",
                "/api/v1/search/posts/prefix",
                "/api/v1/search/posts/fulltext",
                "/api/v1/search/posts/fulltext-boolean"
        );

        for (String path : mysqlPaths) {
            mockMvc.perform(get(path)
                            .servletPath("/api/v1")
                            .param("keyword", "publicsearchtoken")
                            .param("offset", "0")
                            .param("limit", "20"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/search/posts/es")
                        .servletPath("/api/v1")
                        .param("keyword", "publicsearchtoken")
                        .param("offset", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("검색 응답 DTO는 id title summary author viewCount likeCount createdAt 필드를 포함한다")
    void mysqlSearchResponse_matchesDtoContract() throws Exception {
        User author = createUser("search-dto@test.com", "search-author", "search-author");
        String keyword = "searchpublishedcontract";
        createPost(author, keyword + " title", "search summary content");

        mockMvc.perform(get("/api/v1/search/posts")
                        .servletPath("/api/v1")
                        .param("keyword", keyword)
                        .param("offset", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.offset").value(0))
                .andExpect(jsonPath("$.data.limit").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.nextOffset").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].id").isNumber())
                .andExpect(jsonPath("$.data.items[0].title").value(keyword + " title"))
                .andExpect(jsonPath("$.data.items[0].summary").value("search summary content"))
                .andExpect(jsonPath("$.data.items[0].author.id").isNumber())
                .andExpect(jsonPath("$.data.items[0].author.nickname").value("search-author"))
                .andExpect(jsonPath("$.data.items[0].viewCount").value(0))
                .andExpect(jsonPath("$.data.items[0].likeCount").value(0))
                .andExpect(jsonPath("$.data.items[0].createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("DRAFT 게시글은 MySQL 기반 검색 결과에 노출되지 않는다")
    void draftPost_isHiddenFromMysqlSearch() throws Exception {
        User author = createUser("search-draft@test.com", "draft-author", "draft-author");
        String keyword = "searchdrafttoken";
        Post draftPost = createPost(author, keyword + " title", "draft content");
        draftPost.setPostStatus(PostStatus.DRAFT);
        postRepository.save(draftPost);

        assertMysqlSearchDoesNotContain(keyword, keyword + " title");
    }

    @Test
    @DisplayName("PUBLISHED 게시글은 MySQL 기반 검색 결과에 노출된다")
    void publishedPost_isVisibleInMysqlSearch() throws Exception {
        User author = createUser("search-published@test.com", "published-author", "published-author");
        String keyword = "searchpublishedtoken";
        createPost(author, keyword + " title", "published content");

        assertMysqlSearchContains(keyword, keyword + " title");
    }

    @Test
    @DisplayName("삭제된 게시글은 MySQL 기반 검색 결과에 노출되지 않는다")
    void deletedPost_isHiddenFromMysqlSearch() throws Exception {
        User author = createUser("search-deleted@test.com", "deleted-author", "deleted-author");
        String keyword = "searchdeletedtoken";
        Post deletedPost = createPost(author, keyword + " title", "deleted content");
        deletedPost.softDelete();
        postRepository.save(deletedPost);

        assertMysqlSearchDoesNotContain(keyword, keyword + " title");
    }

    @Test
    @DisplayName("ES 검색 API는 공개 접근 가능하고 OffsetResponse DTO 구조를 유지한다")
    void esSearch_isPublicAndMatchesDtoContract() throws Exception {
        PostSearchDocument document = new PostSearchDocument();
        document.setPostId(900L);
        document.setTitle("es published title");
        document.setContent("es summary content");
        document.setAuthorId(91L);
        document.setAuthorNickname("es-author");
        document.setViewCount(12L);
        document.setLikeCount(4L);
        document.setCreatedAt(LocalDateTime.of(2026, 5, 4, 12, 0));
        document.setUpdatedAt(LocalDateTime.of(2026, 5, 4, 12, 5));
        document.setVersion(3L);
        document.setPostStatus("PUBLISHED");

        when(postSearchEsRepository.searchTitle("eskeyword", 0, 20))
                .thenReturn(new EsSearchSliceResult(List.of(document), false));

        mockMvc.perform(get("/api/v1/search/posts/es")
                        .servletPath("/api/v1")
                        .param("keyword", "eskeyword")
                        .param("offset", "0")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].id").value(900))
                .andExpect(jsonPath("$.data.items[0].title").value("es published title"))
                .andExpect(jsonPath("$.data.items[0].summary").value("es summary content"))
                .andExpect(jsonPath("$.data.items[0].author.id").value(91))
                .andExpect(jsonPath("$.data.items[0].author.nickname").value("es-author"))
                .andExpect(jsonPath("$.data.items[0].viewCount").value(12))
                .andExpect(jsonPath("$.data.items[0].likeCount").value(4))
                .andExpect(jsonPath("$.data.items[0].createdAt").isNotEmpty());
    }

    private void assertMysqlSearchContains(String keyword, String title) throws Exception {
        List<String> paths = List.of(
                "/api/v1/search/posts",
                "/api/v1/search/posts/infix",
                "/api/v1/search/posts/prefix",
                "/api/v1/search/posts/fulltext"
        );

        for (String path : paths) {
            mockMvc.perform(get(path)
                            .servletPath("/api/v1")
                            .param("keyword", keyword)
                            .param("offset", "0")
                            .param("limit", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items[*].title", hasItem(title)));
        }
    }

    private void assertMysqlSearchDoesNotContain(String keyword, String title) throws Exception {
        List<String> paths = List.of(
                "/api/v1/search/posts",
                "/api/v1/search/posts/infix",
                "/api/v1/search/posts/prefix",
                "/api/v1/search/posts/fulltext"
        );

        for (String path : paths) {
            mockMvc.perform(get(path)
                            .servletPath("/api/v1")
                            .param("keyword", keyword)
                            .param("offset", "0")
                            .param("limit", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items", hasSize(0)));
        }
    }
}
