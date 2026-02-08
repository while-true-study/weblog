package com.example.blog.search.repository;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTitleSearchRepository extends JpaRepository<Post, Long> {

    // prefix: keyword%
    @EntityGraph(attributePaths = {"author"})
    Slice<Post> findByTitleStartingWithIgnoreCase(String keyword, Pageable pageable);

    // infix: %keyword%
    @EntityGraph(attributePaths = {"author"})
    Slice<Post> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
}
