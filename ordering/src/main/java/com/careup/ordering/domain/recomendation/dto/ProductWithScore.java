package com.careup.ordering.domain.recomendation.dto;

import com.careup.ordering.domain.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductWithScore {
    private final Product product;
    private final double finalScore;
}
