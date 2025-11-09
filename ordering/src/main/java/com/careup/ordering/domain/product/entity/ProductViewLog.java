package com.careup.ordering.domain.product.entity;

import com.careup.ordering.common.domain.BaseTimeEntity;
import com.careup.ordering.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "product_view_log")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductViewLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_view_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id",nullable = false)
    private Member member;

    public static ProductViewLog toEntity(Product product, Member member){
        return ProductViewLog.builder()
                .product(product)
                .member(member)
                .build();
    }

}
