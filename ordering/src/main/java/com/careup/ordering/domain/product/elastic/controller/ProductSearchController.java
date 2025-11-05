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

    @GetMapping("/autocomplete")
    public ResponseEntity<List<AutocompleteResponse>> autocomplete(@RequestParam String keyword) {
        return ResponseEntity.ok(productSearchService.autocomplete(keyword));
    }
}