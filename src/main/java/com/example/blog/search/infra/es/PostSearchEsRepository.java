package com.example.blog.search.infra.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.blog.search.infra.es.document.PostSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostSearchEsRepository { // ES 검색/저장/삭제용

    private static final String INDEX_NAME = "post_search_v4";

    private final ElasticsearchClient elasticsearchClient;

    public void save(PostSearchDocument document) {
        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(String.valueOf(document.getPostId()))
                    .document(document)
            );

            log.info("ES 색인 저장 완료. index={}, postId={}, result={}",
                    INDEX_NAME, document.getPostId(), response.result());

        } catch (ElasticsearchException e) {
            log.error("ES 색인 저장 실패(ElasticsearchException). index={}, postId={}, message={}",
                    INDEX_NAME, document.getPostId(), e.getMessage(), e);
            throw new RuntimeException("ES 색인 저장 실패", e);
        } catch (IOException e) {
            log.error("ES 색인 저장 실패(IOException). index={}, postId={}",
                    INDEX_NAME, document.getPostId(), e);
            throw new RuntimeException("ES 색인 저장 실패", e);
        }
    }

    public void deleteByPostId(Long postId) {
        try {
            DeleteResponse response = elasticsearchClient.delete(d -> d
                    .index(INDEX_NAME)
                    .id(String.valueOf(postId))
            );

            log.info("ES 문서 삭제 완료. index={}, postId={}, result={}",
                    INDEX_NAME, postId, response.result());

        } catch (ElasticsearchException e) {
            log.error("ES 문서 삭제 실패(ElasticsearchException). index={}, postId={}, message={}",
                    INDEX_NAME, postId, e.getMessage(), e);
            throw new RuntimeException("ES 문서 삭제 실패", e);
        } catch (IOException e) {
            log.error("ES 문서 삭제 실패(IOException). index={}, postId={}",
                    INDEX_NAME, postId, e);
            throw new RuntimeException("ES 문서 삭제 실패", e);
        }
    }

    public EsSearchSliceResult searchTitle(String keyword, int offset, int limit) {
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int fetchSize = safeLimit + 1;

        if (keyword == null || keyword.trim().isEmpty()) {
            return new EsSearchSliceResult(List.of(), false);
        }

        if (safeOffset + fetchSize > 10000) {
            throw new IllegalArgumentException("offset + limit exceeds ES max_result_window(10000)");
        }

        try {
            SearchResponse<PostSearchDocument> response = elasticsearchClient.search(s -> s
                            .index(INDEX_NAME)
                            .from(safeOffset)
                            .size(fetchSize)
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.match(mm -> mm
                                            .field("title")
                                            .query(keyword)
                                    ))
                                    .filter(f -> f.term(t -> t
                                            .field("poststatus.keyword")
                                            .value("PUBLISHED")
                                    ))
                            ))
                            .sort(so -> so.field(f -> f.field("createdat").order(SortOrder.Desc)))
                            .sort(so -> so.field(f -> f.field("postid").order(SortOrder.Desc))),
                    PostSearchDocument.class
            );

            List<PostSearchDocument> docs = new ArrayList<>();
            for (Hit<PostSearchDocument> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    docs.add(hit.source());
                }
            }

            boolean hasNext = docs.size() > safeLimit;
            if (hasNext) {
                docs = docs.subList(0, safeLimit);
            }

            return new EsSearchSliceResult(docs, hasNext);

        } catch (ElasticsearchException e) {
            log.error("ES search failed. index={}, keyword={}, offset={}, limit={}, message={}",
                    INDEX_NAME, keyword, safeOffset, safeLimit, e.getMessage(), e);
            throw new RuntimeException("ES 검색 실패(ElasticsearchException)", e);
        } catch (IOException e) {
            log.error("ES search IO failed. index={}, keyword={}, offset={}, limit={}",
                    INDEX_NAME, keyword, safeOffset, safeLimit, e);
            throw new RuntimeException("ES 검색 실패(IOException)", e);
        }
    }


}