package com.careup.ordering.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 카테고리별 속성 정의
 * 특정 카테고리에서 사용할 속성 타입을 지정
 */
@Entity
@Table(name = "category_attribute",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"category_id", "attribute_type_id"})
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CategoryAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_attribute_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_type_id", nullable = false)
    private AttributeType attributeType;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false; // 해당 카테고리에서 필수 여부

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0; // 표시 순서

    @Builder
    public CategoryAttribute(Category category, AttributeType attributeType, 
                           Boolean isRequired, Integer displayOrder) {
        this.category = category;
        this.attributeType = attributeType;
        this.isRequired = isRequired != null ? isRequired : false;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    /**
     * 카테고리 속성 정보 수정
     */
    public void updateInfo(Boolean isRequired, Integer displayOrder) {
        if (isRequired != null) this.isRequired = isRequired;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }
}
