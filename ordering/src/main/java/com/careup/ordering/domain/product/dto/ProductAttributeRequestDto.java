package com.careup.ordering.domain.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttributeRequestDto {
    @NotBlank(message = "속성명은 필수입니다")
    private String attributeName;  // 예: "색상", "사이즈"
    
    @NotBlank(message = "속성값은 필수입니다")
    private String attributeValue;  // 예: "빨강", "L"
}
