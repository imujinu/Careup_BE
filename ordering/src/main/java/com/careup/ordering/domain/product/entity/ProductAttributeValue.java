package com.careup.ordering.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상품의 실제 선택된 속성 값
 * Product와 AttributeValue를 연결
 */
@Entity
@Table(name = "product_attribute_value",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"product_id", "attribute_value_id"})
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ProductAttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_attribute_value_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_value_id", nullable = false)
    private AttributeValue attributeValue;

    @Column(name = "custom_value", length = 100)
    private String customValue; // "기타" 선택 시 사용자 입력값

    @Builder
    public ProductAttributeValue(Product product, AttributeValue attributeValue, String customValue) {
        this.product = product;
        this.attributeValue = attributeValue;
        this.customValue = customValue;
    }

    /**
     * 커스텀 값 수정
     */
    public void updateCustomValue(String customValue) {
        this.customValue = customValue;
    }
}
