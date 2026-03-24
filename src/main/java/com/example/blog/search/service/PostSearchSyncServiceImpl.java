package com.example.blog.search.service;

import com.example.blog.search.infra.es.PostSearchEsRepository;
import com.example.blog.search.infra.es.document.PostSearchDocument;
import com.example.blog.search.outbox.dto.PostOutboxPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostSearchSyncServiceImpl implements PostSearchSyncService {

    private final PostSearchEsRepository postSearchEsRepository;

    @Override
    public void upsert(PostOutboxPayload payload) {
        PostSearchDocument doc = new PostSearchDocument();
        doc.setPostId(payload.getPostId());
        doc.setTitle(payload.getTitle());
        doc.setContentPreview(payload.getContentPreview());
        doc.setAuthorId(payload.getAuthorId());
        doc.setAuthorNickname(payload.getAuthorNickname());
        doc.setViewCount(payload.getViewCount());
        doc.setLikeCount(payload.getLikeCount());
        doc.setCreatedAt(payload.getCreatedAt());
        doc.setUpdatedAt(payload.getUpdatedAt());
        doc.setPostStatus(payload.getPostStatus());
        doc.setVersion(payload.getVersion());

        log.info("ES upsert 요청. postId={}, version={}", payload.getPostId(), payload.getVersion());
        postSearchEsRepository.save(doc);
        log.info("ES upsert 완료. postId={}, version={}", payload.getPostId(), payload.getVersion());
    }

    @Override
    public void delete(Long postId, Long version) {
        log.info("ES delete 요청. postId={}, version={}", postId, version);
        postSearchEsRepository.deleteByPostId(postId);
        log.info("ES delete 완료. postId={}, version={}", postId, version);
    }
}
