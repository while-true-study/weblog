package com.example.blog.search.repository;

import com.example.blog.search.infra.es.EsSearchSliceResult;
import com.example.blog.search.infra.es.document.PostSearchDocument;

import java.util.Optional;

public interface PostSearchRepository {
    void upsertFullDocument(PostSearchDocument document);
    void deleteByPostId(Long postId);
    EsSearchSliceResult searchTitle(String keyword, int offset, int limit);
    Optional<PostSearchDocument> findByPostId(Long postId);
    long count();
}