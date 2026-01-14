package com.example.blog.post.repository;

import com.example.blog.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @Modifying
    @Query("update Post p set p.viewCount = coalesce(p.viewCount, 0) + 1 where p.postId = :id") // coalesce = null이 아닌 첫번째 값 반환
    int incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("select Post p from Post where postStatus = 'PUBLISHED'" )
    List<Post> findPublishPosts();


}
