package com.example.blog.post.repository;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @Modifying
    @Query("update Post p set p.viewCount = coalesce(p.viewCount, 0) + 1 where p.postId = :id") // coalesce = null이 아닌 첫번째 값 반환
    int incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.postId = :id")
    int incrementViewCount2(@Param("id") Long id);

    @Modifying
    @Query("select Post p from Post where postStatus = 'PUBLISHED'" )
    List<Post> findPublishPosts();

    @EntityGraph(attributePaths = {"author"}) // author lazy N+1 줄이기
    Slice<Post> findByPostStatusAndDeletedAtIsNullOrderByPostIdAsc(
            PostStatus postStatus,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Post> findByPostIdAndPostStatusNot(Long postId, PostStatus postStatus);

    @Query("""
    select p
    from Post p
    join fetch p.author
    where p.updatedAt >= :from
""")
    List<Post> findAllUpdatedAfterWithAuthor(@Param("from") LocalDateTime from);

    @Query("select p from Post p join fetch p.author")
    List<Post> findAllWithAuthor();

}
