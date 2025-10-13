package com.careup.ordering.domain.order.entity;

import com.careup.ordering.domain.product.entity.BranchProduct;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ordered_item")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ordered_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_product_id", nullable = false)
    private BranchProduct branchProduct;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    // ✅ 커스텀 Builder 생성자 (totalPrice 자동 계산!)
    @Builder
    public OrderedItem(Order order, BranchProduct branchProduct, Long quantity, Long unitPrice) {
        this.order = order;
        this.branchProduct = branchProduct;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = quantity * unitPrice;  // 자동 계산
    }
}
