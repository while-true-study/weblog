package com.example.blog.post.repository.spec;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.tag.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PostSpecs {
    private PostSpecs() {}

    // Specification는 root, query, builder 이 세가지를 줌
    // 엔티티를 대표하는 기준점, 쿼리 자체, 조건식을 만들어주는 빌더임
    public static Specification<Post> keyword(String keyword) { // 조건 만들기
        return (root, query, cb) -> { // Predicate형태라고 함. (조건식 생성 함수?)
            if (!StringUtils.hasText(keyword)) return cb.conjunction(); // keyword가 null/빈문자/공백이면 항상 true
            String like = "%" + keyword.trim() + "%"; // 앞뒤 공백 제거하고 SQL LIKE로 바꿈 (% ~~~~ % 붙이는것처럼)
            return cb.or( // or
                    cb.like(root.get("title"), like), // title컬럼이 like 패턴을 포함하면 title LIKE '%keyword%'
                    cb.like(root.get("content"), like) // content가 like를 가지고 있으면 content like '%keyword%'
                                // Post.content를 말하는거임
            );
        };
    }


    public static Specification<Post> hasTag(String tag) { // 태그로 가진 Post 조회하는 조건
        return (root, query, cb) -> {
            if (!StringUtils.hasText(tag)) return cb.conjunction();
            query.distinct(true); // join 때문에 중복 row 방지
            Join<Post, Tag> tagJoin = root.join("postTags", JoinType.INNER);
            return cb.equal(tagJoin.get("name"), tag.trim());
        };
    }

    public static Specification<Post> status(PostStatus status) {
        return (root, query, cb) -> {
            if (status == null) return cb.conjunction();
            return cb.equal(root.get("postStatus"), status); // postStatus 속성이 위에 status랑 같은지 published로 줌
        };
    }
}
