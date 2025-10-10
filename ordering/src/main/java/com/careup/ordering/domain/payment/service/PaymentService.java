package com.careup.ordering.domain.payment.service;

import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.payment.dto.PaymentConfirmRequest;
import com.careup.ordering.domain.payment.entity.Payment;
import com.careup.ordering.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${payment.toss.secret-key}")
    private String widgetSecretKey;

    /**
     * 결제 승인
     */
    @Transactional
    public Map<String, Object> confirmPayment(PaymentConfirmRequest request) {
        log.info("결제 승인 시작 - orderId: {}, amount: {}", request.getOrderId(), request.getAmount());

        // 1. 주문 조회 및 금액 검증
        Order order = orderRepository.findById(Long.parseLong(request.getOrderId()))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!order.getTotalAmount().equals(request.getAmount())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        // 2. 토스페이먼츠 API 호출
        Map<String, Object> tossResponse = callTossPaymentsApi(request);

        // 3. Payment 저장
        savePayment(order, request, tossResponse);

        log.info("결제 승인 완료 - orderId: {}", request.getOrderId());

        return tossResponse;
    }

    /**
     * 토스페이먼츠 API 호출
     */
    private Map<String, Object> callTossPaymentsApi(PaymentConfirmRequest request) {
        try {
            // Authorization 헤더 생성
            String encodedAuth = Base64.getEncoder()
                    .encodeToString((widgetSecretKey + ":").getBytes(StandardCharsets.UTF_8));

            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 요청 바디
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("paymentKey", request.getPaymentKey());
            requestBody.put("orderId", request.getOrderId());
            requestBody.put("amount", request.getAmount());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 토스 API 호출
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.tosspayments.com/v1/payments/confirm",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("토스페이먼츠 API 호출 실패");
            }

            return response.getBody();

        } catch (Exception e) {
            log.error("토스페이먼츠 API 호출 실패", e);
            throw new RuntimeException("결제 승인 실패: " + e.getMessage());
        }
    }

    /**
     * Payment 저장
     */
    private void savePayment(Order order, PaymentConfirmRequest request, Map<String, Object> tossResponse) {
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseGet(() -> Payment.builder()
                        .order(order)
                        .amount(request.getAmount())
                        .build());

        payment.confirm(
                request.getPaymentKey(),
                (String) tossResponse.get("transactionKey")
        );

        paymentRepository.save(payment);
    }

    /**
     * 결제 조회
     */
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다."));
    }

    /**
     * 주문별 결제 조회
     */
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문의 결제 정보가 없습니다."));
    }
}