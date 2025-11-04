package com.careup.ordering.domain.product.elastic.controller;

import com.careup.ordering.domain.product.elastic.dto.AutocompleteResponse;
import com.careup.ordering.domain.product.elastic.dto.ProductSearchRequest;
import com.careup.ordering.domain.product.elastic.dto.ProductSearchResponse;
import com.careup.ordering.domain.product.elastic.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products/es-search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService productSearchService;

    /**
     * Elasticsearch 기반 상품 검색 API
     * GET /products/es-search?keyword=노트북&categoryId=1&minPrice=10000&maxPrice=50000&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<Page<ProductSearchResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ProductSearchRequest request = ProductSearchRequest.builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .page(page)
                .size(size)
                .build();

        Page<ProductSearchResponse> result = productSearchService.searchProducts(request);
        return ResponseEntity.ok(result);
    }

    /**
     * Elasticsearch 기반 자동완성 API
     * GET /products/es-search/autocomplete?keyword=아메
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<AutocompleteResponse>> autocomplete(
            @RequestParam String keyword
    ) {
        List<AutocompleteResponse> result = productSearchService.autocomplete(keyword);
        return ResponseEntity.ok(result);
    }
}

