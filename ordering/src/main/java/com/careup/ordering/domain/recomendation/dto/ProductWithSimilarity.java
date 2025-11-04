package com.careup.ordering.domain.recomendation.dto;

import com.careup.ordering.domain.product.entity.Product;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductWithSimilarity {
    private Product product;
    private double similarityScore;
}