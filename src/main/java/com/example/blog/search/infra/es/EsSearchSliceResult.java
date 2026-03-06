package com.example.blog.search.infra.es;

import com.example.blog.search.infra.es.document.PostSearchDocument;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class EsSearchSliceResult {
    private final List<PostSearchDocument> items;
    private final boolean hasNext;
}