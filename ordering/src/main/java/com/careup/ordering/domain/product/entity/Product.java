package com.careup.ordering.domain.product.entity;

import com.careup.ordering.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name= "product")
@AllArgsConstructor
@NoArgsConstructor
@Getter
// BaseTimeEntity 추가예정
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "category_id",nullable = false)
    private Category category;

    @Column(name = "name",nullable = false,length = 100)
    private String name;

    @Column(name = "description",nullable = false,columnDefinition = "TEXT")
    private String description;

    @Column(name = "supply_price", nullable = false)
    private Long supplyPrice; // 본사 -> 가맹점 공급가

    @Column(name = "min_price",nullable = false)
    private Long minPrice; // 소비자 판매 권장 최저가

    @Column(name = "max_price")
    private Long maxPrice; // 소비자 판매 권장 최고가

    @Column(name = "image_url",nullable = false,length = 255)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "is_del_yn", nullable = false, length = 1)
    private String isDelYn = "N";

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private Visibility visibility;

    @OneToMany(mappedBy = "product",cascade = CascadeType.ALL)
    private List<ProductAttribute> attributes = new ArrayList<>();

    @Column(name = "view_count", nullable = true, columnDefinition = "BIGINT DEFAULT 0")
    private Long viewCount = 0L;

    @Builder
    public Product(Category category, String name, String description,
                   Long supplyPrice, Long minPrice, Long maxPrice, String imageUrl, Visibility visibility) {
        this.category = category;
        this.name = name;
        this.description = description;
        this.supplyPrice = supplyPrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.imageUrl = imageUrl;
        this.visibility = visibility != null ? visibility : Visibility.ALL;
    }

    // 삭제 메서드
    public void delete() {
        this.isDelYn = "Y";
    }

    // 활성화 상태 체크
    public boolean isActive() {
        return this.status == ProductStatus.ACTIVE && "N".equals(this.isDelYn);
    }

    // 상품 수정 메서드
    public void updateInfo(String name, String description, Long supplyPrice, Long minPrice, Long maxPrice, String imageUrl) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (supplyPrice != null) this.supplyPrice = supplyPrice;
        if (minPrice != null) this.minPrice = minPrice;
        if (maxPrice != null) this.maxPrice = maxPrice;
        if (imageUrl != null) this.imageUrl = imageUrl;
    }

    public void plusCount(){
        this.viewCount++;
    }
}
