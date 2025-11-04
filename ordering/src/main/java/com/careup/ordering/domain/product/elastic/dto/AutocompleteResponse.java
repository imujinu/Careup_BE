package com.careup.ordering.domain.product.elastic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutocompleteResponse {
    private Long id;
    private String name;
}

