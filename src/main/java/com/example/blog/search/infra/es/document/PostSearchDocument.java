package com.example.blog.search.infra.es.document;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostSearchDocument {

    @JsonProperty("postid")
    private Long postId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("contentpreview")
    private String contentPreview;

    @JsonProperty("authorid")
    private Long authorId;

    // 현재 Logstash에서 안 넣고 있으면 null로 들어오는 게 정상
    @JsonProperty("authornickname")
    private String authorNickname;

    @JsonProperty("viewcount")
    private Long viewCount;

    @JsonProperty("likecount")
    private Long likeCount;

    @JsonProperty("createdat")
    private String createdAt; // 나중에 필요하면 LocalDateTime/Instant로 변경 가능

    @JsonProperty("poststatus")
    private String postStatus;
}