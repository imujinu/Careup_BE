package com.careup.ordering.domain.product.elastic.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NumberRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.careup.ordering.domain.product.elastic.document.ProductDocument;
import com.careup.ordering.domain.product.elastic.dto.AutocompleteResponse;
import com.careup.ordering.domain.product.elastic.dto.ProductSearchRequest;
import com.careup.ordering.domain.product.elastic.dto.ProductSearchResponse;
import com.careup.ordering.domain.product.elastic.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 상품 검색
     */
    public Page<ProductSearchResponse> searchProducts(ProductSearchRequest request) {
        List<Query> mustQueries = new ArrayList<>();

        // 키워드 검색: name(가중치3), description(1), categoryName(2)
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                    .query(request.getKeyword())
                    .fields("name^3", "description^1", "categoryName^2")
                    .fuzziness("AUTO")
                    .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.Or)
            );
            mustQueries.add(Query.of(q -> q.multiMatch(multiMatchQuery)));
        }

        // 카테고리 필터
        if (request.getCategoryId() != null) {
            MatchQuery categoryQuery = MatchQuery.of(m -> m
                    .field("categoryId")
                    .query(request.getCategoryId())
            );
            mustQueries.add(Query.of(q -> q.match(categoryQuery)));
        }

        // 가격 범위 필터 (NPE 가드)
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            NumberRangeQuery.Builder rb = new NumberRangeQuery.Builder().field("price");
            if (request.getMinPrice() != null) rb.gte(request.getMinPrice().doubleValue());
            if (request.getMaxPrice() != null) rb.lte(request.getMaxPrice().doubleValue());
            mustQueries.add(rb.build()._toRangeQuery()._toQuery());
        }

        BoolQuery boolQuery = BoolQuery.of(b -> b.must(mustQueries));

        // Highlight (상품명만)
        List<HighlightField> highlightFields = List.of(new HighlightField("name"));
        HighlightParameters highlightParameters = HighlightParameters.builder()
                .withPreTags("<b>").withPostTags("</b>").build();
        Highlight highlight = new Highlight(highlightParameters, highlightFields);
        HighlightQuery highlightQuery = new HighlightQuery(highlight, null);

        // Native Query
        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQuery)))
                .withPageable(PageRequest.of(request.getPage(), request.getSize()))
                .withHighlightQuery(highlightQuery)
                .build();

        // 검색 실행
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

        // 결과 변환
        List<ProductSearchResponse> responses = searchHits.getSearchHits().stream()
                .map(hit -> {
                    ProductDocument doc = hit.getContent();
                    String highlightedName = doc.getName();
                    if (!hit.getHighlightFields().isEmpty()
                            && hit.getHighlightField("name") != null
                            && !hit.getHighlightField("name").isEmpty()) {
                        highlightedName = hit.getHighlightField("name").get(0);
                    }
                    return ProductSearchResponse.builder()
                            .id(doc.getProductId())
                            .name(doc.getName())
                            .description(doc.getDescription())
                            .categoryName(doc.getCategoryName())
                            .price(doc.getPrice())
                            .imageUrl(doc.getImageUrl())
                            .highlightedName(highlightedName)
                            .build();
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, PageRequest.of(request.getPage(), request.getSize()), searchHits.getTotalHits());
    }

    /**
     * 자동완성 (상품명만, 최대 5개)
     */
    public List<AutocompleteResponse> autocomplete(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();

        MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                .query(keyword)
                .fields("name.autocomplete", "name.autocomplete._2gram", "name.autocomplete._3gram")
                .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BoolPrefix)
        );

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.multiMatch(multiMatchQuery)))
                .withPageable(PageRequest.of(0, 5))
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(searchQuery, ProductDocument.class);

        return searchHits.getSearchHits().stream()
                .map(hit -> AutocompleteResponse.builder()
                        .id(hit.getContent().getProductId())
                        .name(hit.getContent().getName())
                        .build())
                .collect(Collectors.toList());
    }

    public void saveProductDocument(ProductDocument document) {
        productSearchRepository.save(document);
        log.info("Product document saved: {}", document.getProductId());
    }

    public void deleteProductDocument(Long productId) {
        productSearchRepository.deleteById(String.valueOf(productId));
        log.info("Product document deleted: {}", productId);
    }
}