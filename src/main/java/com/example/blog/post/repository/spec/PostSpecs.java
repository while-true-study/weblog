package com.example.blog.post.repository.spec;

import com.example.blog.post.entity.Post;
import com.example.blog.tag.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PostSpecs {
    private PostSpecs() {}

    public static Specification<Post> keyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return cb.conjunction();
            String like = "%" + keyword.trim() + "%";
            return cb.or(
                    cb.like(root.get("title"), like),
                    cb.like(root.get("content"), like)
            );
        };
    }

    public static Specification<Post> categoryId(Long categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) return cb.conjunction();
            return cb.equal(root.get("category").get("id"), categoryId);
        };
    }

    public static Specification<Post> hasTag(String tag) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(tag)) return cb.conjunction();
            query.distinct(true); // join 때문에 중복 row 방지
            Join<Post, Tag> tagJoin = root.join("postTags", JoinType.INNER);
            return cb.equal(tagJoin.get("name"), tag.trim());
        };
    }
}
