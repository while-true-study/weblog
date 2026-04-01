package com.example.blog.popular.batch;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PopularPostRankItem {
    private Long postId;
    private int rank;
    private double score;
}