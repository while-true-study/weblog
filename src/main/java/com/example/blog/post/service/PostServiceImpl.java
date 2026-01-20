package com.example.blog.post.service;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.post.presentation.dto.request.PostPublishedDto;
import com.example.blog.post.presentation.dto.response.PostCreateResponse;
import com.example.blog.post.presentation.dto.response.PostDetailDto;
import com.example.blog.post.presentation.dto.response.PostListItemDto;
import com.example.blog.post.presentation.dto.response.PostSearchCond;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.post.repository.spec.PostSpecs;
import com.example.blog.series.entity.Series;
import com.example.blog.series.repository.SeriesRepository;
import com.example.blog.tag.entity.Tag;
import com.example.blog.tag.repository.TagRepository;
import com.example.blog.user.entity.User;
import com.example.blog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final SeriesRepository seriesRepository;

    @Override
    public Page<PostListItemDto> getPosts(PostSearchCond cond, Pageable pageable) {
        Specification<Post> spec =
                PostSpecs.keyword(cond.keyword())
                        .and(PostSpecs.categoryId(cond.categoryId()))
                        .and(PostSpecs.hasTag(cond.tag()))
                        .and(PostSpecs.status(PostStatus.PUBLISHED));


        return postRepository.findAll(spec, pageable)
                .map(PostListItemDto::from);
    }

    @Override
    @Transactional
    public PostDetailDto getPost(Long id) {
        postRepository.incrementViewCount(id); // 조회수 증가
        return PostDetailDto.from(postRepository.findById(id).orElse(null));
    }

    @Override
    public PostCreateResponse createPost(PostPublishedDto postPublishedDto, String username) {
        User author = userRepository.findByEmail(username) // 또는 findByUsername
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없음"));

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
            Series series = seriesRepository.findByIdAndOwner_UserId(postPublishedDto.seriesId(), author.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("내 시리즈가 아니거나 존재하지 않음"));
            post.setSeries(series);
        }

        post.clearTags();

        if (postPublishedDto.tags() != null && !postPublishedDto.tags().isEmpty()) { // 만약에 tag가 있으면
            postPublishedDto.tags().stream() // List < String > 이기 때문에 stream
                    .filter(t -> t != null && !t.isBlank()) // 만약에 "" 이런게 있으면 버림
                    .map(String::trim) // 공백 없앰
                    .distinct() // 중복 없앰
                    .forEach(tagName -> {
                        Tag tag = tagRepository.findByTagName(tagName) // String 이라서 찾아야함
                                .orElseGet(() -> tagRepository.save(Tag.of(tagName)));// 만약에 새로운거면(못찾으면)Tag테이블에 저장함
                        post.addTag(tag); // 게시글에도 태그 추가함
                    });
        }
        Post saved = postRepository.save(post);
        return PostCreateResponse.from(saved);
    }
}
