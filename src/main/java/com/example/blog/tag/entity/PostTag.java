package com.example.blog.tag.entity;

import com.example.blog.post.entity.Post;
import com.example.blog.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "post_tags")
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postTagId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Post postId;

    @ManyToOne(fetch = FetchType.LAZY)
    private User userId;

}
