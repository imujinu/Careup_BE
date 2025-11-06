package com.careup.ordering.domain.recomendation.entity;

import com.careup.ordering.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCoPurchase extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_a_id", nullable = false)
    private Long productAId;

    @Column(name = "product_b_id", nullable = false)
    private Long productBId;

    @Column(name = "co_purchase_count", nullable = false)
    private Long coPurchaseCount;


    public void increaseCount() {
        this.coPurchaseCount++;
    }
}
