package com.example.blog.search.repository;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostTitleSearchRepository extends JpaRepository<Post, Long> {

    // prefix: keyword%
    @EntityGraph(attributePaths = {"author"})
    Slice<Post> findByTitleStartingWithIgnoreCase(String keyword, Pageable pageable);

    // infix: %keyword%
    @EntityGraph(attributePaths = {"author"})
    Slice<Post> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("""
            select p
            from Post p
            join fetch p.author a
            where lower(p.title) like lower(concat(:keyword, '%'))
            order by p.createdAt desc, p.postId desc
            """)
    Slice<Post> searchTitlePrefix(@Param("keyword") String keyword, Pageable pageable); // keword%

    @Query("""
            select p
            from Post p
            join fetch p.author a
            where lower(p.title) like lower(concat('%', :keyword, '%'))
            order by p.createdAt desc, p.postId desc
            """)
    Slice<Post> searchTitleInfix(@Param("keyword") String keyword, Pageable pageable); //  %keword%
}
