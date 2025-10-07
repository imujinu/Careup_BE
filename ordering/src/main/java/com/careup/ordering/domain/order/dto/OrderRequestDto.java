package com.careup.ordering.domain.order.dto;

import com.careup.ordering.domain.order.entity.OrderType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDto {
    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @NotNull(message = "지점ID는 필수입니다.")
    private Long branchId;

    @NotNull(message = "주문 타입을 필수입니다")
    private OrderType orderType;

    @NotNull(message = "주문 타입은 필수입니다")
    private List<OrderItemRequestDto> orderItems;

    // 쿠폰적용시 -> 없어질 가능성 높음.
    private Long couponId;
}
