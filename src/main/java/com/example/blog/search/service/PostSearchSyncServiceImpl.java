package com.example.blog.search.service;

import com.example.blog.post.entity.Post;
import com.example.blog.post.entity.PostStatus;
import com.example.blog.post.repository.PostRepository;
import com.example.blog.search.infra.es.PostSearchEsRepository;
import com.example.blog.search.infra.es.document.PostSearchDocument;
import com.example.blog.search.outbox.dto.PostOutboxPayload;
import com.example.blog.global.exception.BlogException;
import com.example.blog.global.exception.ErrorCode;
import com.example.blog.search.repository.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostSearchSyncServiceImpl implements PostSearchSyncService {

    private final PostRepository postRepository;
    private final PostSearchRepository postSearchRepository;

    @Override
    @Transactional
    public void syncPostToSearch(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BlogException(ErrorCode.POST_NOT_FOUND));

        // Search exposure policy:
        // - DRAFT is not exposed by either MySQL or ES search endpoints.
        // - DELETED is removed from ES by delete events. If a delete event fails, stale docs can remain
        //   until retry/repair processing catches up.
        if (post.getPostStatus() == PostStatus.DELETED) {
            postSearchRepository.deleteByPostId(postId);
            return;
        }

        // Official document mapping path for search sync.
        PostSearchDocument document = PostSearchDocument.from(post);
        postSearchRepository.upsertFullDocument(document);

        log.info("ES 문서 동기화 완료. postId={}, version={}", postId, document.getVersion());
    }

    @Transactional
    public void delete(Long postId, Long version) {
        postSearchRepository.deleteByPostId(postId);
    }
}
