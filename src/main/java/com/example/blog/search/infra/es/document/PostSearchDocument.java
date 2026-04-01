package com.example.blog.search.infra.es.document;

import com.example.blog.post.entity.Post;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Document(indexName = "post_search_v4")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostSearchDocument {

    @Id
    @JsonProperty("postid")
    private Long postId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @JsonProperty("authorid")
    private Long authorId;

    @JsonProperty("authornickname")
    private String authorNickname;

    @JsonProperty("version")
    private Long version;

    @JsonProperty("viewcount")
    private Long viewCount;

    @JsonProperty("likecount")
    private Long likeCount;

    @JsonProperty("createdat")
    private LocalDateTime createdAt;

    @JsonProperty("updatedat")
    private LocalDateTime updatedAt;

    @JsonProperty("poststatus")
    private String postStatus;

    public static PostSearchDocument from(Post post) {
        PostSearchDocument doc = new PostSearchDocument();
        doc.setPostId(post.getPostId());
        doc.setTitle(post.getTitle());
        doc.setContent(post.getContent());
        doc.setAuthorId(post.getAuthor().getUserId());
        doc.setAuthorNickname(post.getAuthor().getNickname());
        doc.setVersion(post.getSyncVersion());
        doc.setViewCount(post.getViewCount());
        doc.setLikeCount(post.getLikeCount());
        doc.setCreatedAt(post.getCreatedAt());
        doc.setUpdatedAt(post.getUpdatedAt());
        doc.setPostStatus(post.getPostStatus().name());
        return doc;
    }
}