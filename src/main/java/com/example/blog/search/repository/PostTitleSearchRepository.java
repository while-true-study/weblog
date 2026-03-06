package com.example.blog.search.repository;

import com.example.blog.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostTitleSearchRepository
        extends JpaRepository<Post, Long>, PostTitleSearchCustomRepository {

    // prefix: keyword%
    @Query("""
            select p
            from Post p
            join fetch p.author a
            where p.postStatus = com.example.blog.post.entity.PostStatus.PUBLISHED
              and p.deletedAt is null
              and p.title like concat(:keyword, '%')
            """)
    Slice<Post> searchTitlePrefix(@Param("keyword") String keyword, Pageable pageable);

    // infix: %keyword%
    @Query("""
            select p
            from Post p
            join fetch p.author a
            where p.postStatus = com.example.blog.post.entity.PostStatus.PUBLISHED
              and p.deletedAt is null
              and p.title like concat('%', :keyword, '%')
            """)
    Slice<Post> searchTitleInfix(@Param("keyword") String keyword, Pageable pageable);

    // ↓↓↓ 기존 native Page<Post> fulltext 메서드들은 삭제/미사용 처리 권장 ↓↓↓
    // Page<Post> searchTitleFulltext(...)
    // Page<Post> searchTitleFulltextBoolean(...)
}