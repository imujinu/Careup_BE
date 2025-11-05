package com.careup.ordering.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 속성 타입 템플릿
 * 예: 색상, 용도, 계절, 성별 등
 */
@Entity
@Table(name = "attribute_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AttributeType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attribute_type_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name; // 색상, 용도, 계절, 성별

    @Column(name = "description", length = 255)
    private String description; // 설명

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false; // 필수 속성 여부

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0; // 표시 순서

    @OneToMany(mappedBy = "attributeType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttributeValue> attributeValues = new ArrayList<>();

    @OneToMany(mappedBy = "attributeType", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoryAttribute> categoryAttributes = new ArrayList<>();

    @Builder
    public AttributeType(String name, String description, Boolean isRequired, Integer displayOrder) {
        this.name = name;
        this.description = description;
        this.isRequired = isRequired != null ? isRequired : false;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    /**
     * 속성 타입 정보 수정
     */
    public void updateInfo(String name, String description, Boolean isRequired, Integer displayOrder) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (isRequired != null) this.isRequired = isRequired;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }
}
