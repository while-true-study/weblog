package com.example.blog.search.infra.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
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
public class PostSearchEsRepository {

    private static final String INDEX_NAME = "post_search_v4";

    private final ElasticsearchClient elasticsearchClient;

    public EsSearchSliceResult searchTitle(String keyword, int offset, int limit) {
        int safeOffset = Math.max(offset, 0);
        int safeLimit = Math.max(1, Math.min(limit, 100)); // 과도한 limit 방지
        int fetchSize = safeLimit + 1; // hasNext 계산용

        // 빈 검색어 방어
        if (keyword == null || keyword.trim().isEmpty()) {
            return new EsSearchSliceResult(List.of(), false);
        }

        // ES 기본 max_result_window(10000) 방어 (from + size)
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
                                            // poststatus가 text+keyword로 매핑된 경우 안전
                                            .field("poststatus.keyword")
                                            .value("PUBLISHED")
                                    ))
                            ))
                            .sort(so -> so.field(f -> f.field("createdat").order(SortOrder.Desc)))
                            .sort(so -> so.field(f -> f.field("postid").order(SortOrder.Desc)))
                    ,
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