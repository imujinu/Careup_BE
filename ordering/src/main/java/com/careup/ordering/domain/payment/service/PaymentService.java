package com.careup.ordering.domain.payment.service;

import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.payment.dto.PaymentConfirmRequest;
import com.careup.ordering.domain.payment.entity.Payment;
import com.careup.ordering.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Value("${payment.toss.secret-key}")
    private String tossSecretKey;

    /**
     * 결제 승인 (토스페이먼츠 공식 샘플 코드 기반)
     */
    @Transactional
    public Map<String, Object> confirmPayment(PaymentConfirmRequest request) throws Exception {
        log.info("결제 승인 시작 - orderId: {}, paymentKey: {}, amount: {}", 
                request.getOrderId(), request.getPaymentKey(), request.getAmount());

        // 1. 주문 조회 및 금액 검증
        Order order = orderRepository.findById(Long.parseLong(request.getOrderId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 주문입니다. orderId: " + request.getOrderId()));

        if (!order.getTotalAmount().equals(request.getAmount())) {
            throw new IllegalArgumentException(
                    String.format("결제 금액이 일치하지 않습니다. 주문금액: %d, 결제금액: %d", 
                            order.getTotalAmount(), request.getAmount()));
        }

        // 2. 토스페이먼츠 API 호출 (공식 샘플 코드 방식)
        JSONObject responseData = callTossPaymentsApi(request);

        // 3. Payment 엔티티 저장
        Payment payment = Payment.builder()
                .order(order)
                .amount(request.getAmount())
                .build();

        payment.confirm(request.getPaymentKey(), (String) responseData.get("transactionKey"));
        paymentRepository.save(payment);

        log.info("결제 승인 완료 - paymentId: {}, orderId: {}", payment.getId(), order.getId());

        // 4. 응답 데이터 변환
        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", payment.getId());
        result.put("orderId", order.getId());
        result.put("amount", payment.getAmount());
        result.put("paymentKey", payment.getPaymentKey());
        result.put("status", payment.getPaymentStatus().name());
        result.put("tossResponse", responseData);

        return result;
    }

    /**
     * 토스페이먼츠 API 호출 (공식 샘플 코드)
     */
    @SuppressWarnings("unchecked") // 토스 페이먼츠 공식에서 가져왔는데 경고가 떠서 경고문 뜨는거 없어지는 코드
    private JSONObject callTossPaymentsApi(PaymentConfirmRequest request) throws Exception {
        // 요청 데이터 생성
        JSONObject requestData = new JSONObject();
        requestData.put("orderId", request.getOrderId());
        requestData.put("amount", request.getAmount());
        requestData.put("paymentKey", request.getPaymentKey());

        // Authorization 헤더 생성 (Basic Auth)
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] encodedBytes = encoder.encode((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        String authorization = "Basic " + new String(encodedBytes, StandardCharsets.UTF_8);

        // HTTP 연결 설정
        URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", authorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        // 요청 바디 전송
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(requestData.toString().getBytes(StandardCharsets.UTF_8));
        }

        // 응답 처리
        int responseCode = connection.getResponseCode();
        boolean isSuccess = (responseCode == 200);

        InputStream responseStream = isSuccess ? 
                connection.getInputStream() : connection.getErrorStream();

        // JSON 파싱
        JSONParser parser = new JSONParser();
        JSONObject responseData;
        try (Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8)) {
            responseData = (JSONObject) parser.parse(reader);
        }

        // 에러 처리
        if (!isSuccess) {
            String errorCode = (String) responseData.get("code");
            String errorMessage = (String) responseData.get("message");
            log.error("토스페이먼츠 API 에러 - code: {}, message: {}", errorCode, errorMessage);
            throw new RuntimeException("결제 승인 실패: " + errorMessage);
        }

        log.info("토스페이먼츠 API 호출 성공 - responseCode: {}", responseCode);
        return responseData;
    }

    /**
     * Payment 단건 조회
     */
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 결제입니다. ID: " + paymentId));
    }

    /**
     * 주문별 Payment 조회
     */
    public Payment getPaymentByOrderId(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 주문입니다. ID: " + orderId));

        return paymentRepository.findByOrder(order)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 주문의 결제 정보가 없습니다. orderId: " + orderId));
    }
    // 회원별 결제 목록 조회
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByMemberId(Long memberId) {
        return paymentRepository.findByOrder_Member_Id(memberId);
    }

    // 결제 삭제 (취소)
    @Transactional
    public void deletePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다. ID: " + paymentId));

        paymentRepository.delete(payment);
        log.info("결제 삭제 완료 - paymentId: {}", paymentId);
    }
}
