package com.example.blog.search.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RepairResult {
    private int reindexed;
    private int deleted;
    private int skipped;
}