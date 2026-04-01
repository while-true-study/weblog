package com.example.blog.comment.repository;

import com.example.blog.comment.entity.Comment;
import com.example.blog.post.entity.Post;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author", "post"})
    List<Comment> findAllByPostOrderByCreatedAtAsc(Post post);

    @EntityGraph(attributePaths = {"author", "post"})
    java.util.Optional<Comment> findWithAuthorAndPostById(Long id);
}