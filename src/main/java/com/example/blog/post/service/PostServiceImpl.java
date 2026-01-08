package com.example.blog.post.service;

import com.example.blog.post.entity.Post;
import com.example.blog.post.presentation.dto.response.PostDetailDto;
import com.example.blog.post.presentation.dto.response.PostListItemDto;
import com.example.blog.post.presentation.dto.response.PostSearchCond;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.post.repository.spec.PostSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    @Override
    public Page<PostListItemDto> getPosts(PostSearchCond cond, Pageable pageable) {
        Specification<Post> spec =
                PostSpecs.keyword(cond.keyword())
                        .and(PostSpecs.categoryId(cond.categoryId()))
                        .and(PostSpecs.hasTag(cond.tag()));

        return postRepository.findAll(spec, pageable)
                .map(PostListItemDto::from);
    }

    @Override
    @Transactional
    public PostDetailDto getPost(Long id) {
        postRepository.incrementViewCount(id);
        return PostDetailDto.from(postRepository.findById(id).orElse(null));
    }
}
