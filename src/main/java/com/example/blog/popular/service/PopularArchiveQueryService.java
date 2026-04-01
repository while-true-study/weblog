package com.example.blog.popular.service;

import com.example.blog.popular.entity.PopularPostDaily;
import com.example.blog.popular.presentation.dto.response.PopularPostResponse;
import com.example.blog.popular.repository.PopularPostDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularArchiveQueryService {

    private final PopularPostDailyRepository popularPostDailyRepository;

    public List<PopularPostResponse> getArchivedDailyPopularPosts(LocalDate targetDate) {
        List<PopularPostDaily> items =
                popularPostDailyRepository.findByTargetDateOrderByRankNoAsc(targetDate);

        return items.stream()
                .map(item -> PopularPostResponse.builder()
                        .postId(item.getPost().getPostId())
                        .title(item.getPost().getTitle())
                        .summary(item.getPost().getContent())
                        .authorNickname(item.getPost().getAuthor().getNickname())
                        .likeCount(item.getPost().getLikeCount())
                        .viewCount(item.getPost().getViewCount())
                        .popularScore(item.getScore())
                        .rank(item.getRankNo())
                        .build())
                .toList();
    }
}