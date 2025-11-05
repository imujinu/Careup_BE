package com.careup.ordering.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 속성 값 템플릿
 * 예: white, black, gray, 러닝, 헬스 등
 */
@Entity
@Table(name = "attribute_value")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttributeValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attribute_value_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_type_id", nullable = false)
    private AttributeType attributeType;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName; // 속성 값 이름 (화면 표시)

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0; // 표시 순서

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // 활성화 여부

    @OneToMany(mappedBy = "attributeValue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttributeValue> productAttributeValues = new ArrayList<>();

    @Builder
    public AttributeValue(AttributeType attributeType, String displayName, 
                         Integer displayOrder, Boolean isActive) {
        this.attributeType = attributeType;
        this.displayName = displayName;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * 속성 값 정보 수정
     */
    public void updateInfo(String displayName, Integer displayOrder, Boolean isActive) {
        if (displayName != null) this.displayName = displayName;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (isActive != null) this.isActive = isActive;
    }

    /**
     * 활성화 상태 변경
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }
}
