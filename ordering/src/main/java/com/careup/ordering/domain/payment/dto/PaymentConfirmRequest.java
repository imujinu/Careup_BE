package com.careup.ordering.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토스페이먼츠 결제 승인 요청 DTO
 * 프론트엔드에서 /confirm API로 전송하는 데이터
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentConfirmRequest {
    private String paymentKey;  // 토스페이먼츠에서 발급한 결제 키
    private String orderId;     // 주문 ID (Order.id를 String으로)
    private String tossOrderId; // 토스페이먼츠에 전송한 원본 orderId (타임스탬프 포함, 예: CAREUP_ORDER_4_1761756734173)
    private Long amount;        // 결제 금액
}
