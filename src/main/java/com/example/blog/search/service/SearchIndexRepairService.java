package com.example.blog.search.service;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.search.batch.dto.RepairResult;
import com.example.blog.search.infra.es.document.PostSearchDocument;
import com.example.blog.search.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SearchIndexRepairService {

    private final PostRepository postRepository;
    private final PostSearchRepository postSearchRepository;
    private final PostSearchSyncService postSearchSyncService;

    /**
     * 전체 Post를 대상으로 ES 인덱스를 동기화합니다.
     * 앱 시작 시 또는 대규모 불일치 복구 시 사용합니다.
     */
    @Transactional(readOnly = true)
    public RepairResult repairAll() {
        List<Post> posts = postRepository.findAllWithAuthor();
        return process(posts);
    }

    /**
     * 특정 시점 이후 변경된 Post만 대상으로 ES 인덱스를 동기화합니다.
     */
    @Transactional(readOnly = true)
    public RepairResult repair(LocalDateTime from) {
        List<Post> posts = postRepository.findAllUpdatedAfterWithAuthor(from);
        return process(posts);
    }

    private RepairResult process(List<Post> posts) {
        int reindexed = 0;
        int deleted = 0;
        int skipped = 0;

        for (Post post : posts) {
            Long postId = post.getPostId();
            PostSearchDocument esDoc = postSearchRepository.findByPostId(postId)
                    .orElse(null);

            boolean deletedStatus = post.getPostStatus() == PostStatus.DELETED;

            if (deletedStatus) {
                if (esDoc != null) {
                    postSearchRepository.deleteByPostId(postId);
                    deleted++;
                } else {
                    skipped++;
                }
                continue;
            }

            if (esDoc == null) {
                postSearchSyncService.syncPostToSearch(postId);
                reindexed++;
                continue;
            }

            if (!Objects.equals(post.getSyncVersion(), esDoc.getVersion())) {
                postSearchSyncService.syncPostToSearch(postId);
                reindexed++;
            } else {
                skipped++;
            }
        }

        return new RepairResult(reindexed, deleted, skipped);
    }
}
