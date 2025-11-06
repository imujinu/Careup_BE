package com.careup.ordering.domain.payment.service;

import com.careup.ordering.config.TossPaymentConfig;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.order.service.OrderService;
import com.careup.ordering.domain.payment.dto.PaymentConfirmRequest;
import com.careup.ordering.domain.payment.entity.Payment;
import com.careup.ordering.domain.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentConfig tossPaymentConfig;  //  Config 주입
    private final OrderService orderService;  // 주문 취소 및 재고 복구용

    // 순환 참조 방지를 위해 @Lazy 사용
    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            TossPaymentConfig tossPaymentConfig,
            @Lazy OrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.tossPaymentConfig = tossPaymentConfig;
        this.orderService = orderService;
    }

    /**
     * 결제 승인 (토스페이먼츠 공식 샘플 코드 기반)
     */
    @Transactional
    public Map<String, Object> confirmPayment(PaymentConfirmRequest request) throws Exception {
        log.info("결제 승인 시작 - orderId: {}, tossOrderId: {}, paymentKey: {}, amount: {}", 
                request.getOrderId(), request.getTossOrderId(), request.getPaymentKey(), request.getAmount());

        // 1. 주문 조회 및 금액 검증
        Order order = orderRepository.findById(Long.parseLong(request.getOrderId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 주문입니다. orderId: " + request.getOrderId()));

        // 주문 상태 확인 - 취소된 주문은 결제 불가
        if (order.getOrderStatus() == com.careup.ordering.domain.order.entity.OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "이미 취소된 주문입니다. 주문이 타임아웃되어 취소되었습니다. 새로운 주문을 생성해주세요. orderId: " + order.getId());
        }

        // PENDING 상태가 아닌 주문은 결제 불가
        if (order.getOrderStatus() != com.careup.ordering.domain.order.entity.OrderStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("결제 가능한 상태가 아닙니다. 현재 주문 상태: %s (orderId: %d)", 
                            order.getOrderStatus(), order.getId()));
        }

        if (!order.getTotalAmount().equals(request.getAmount())) {
            throw new IllegalArgumentException(
                    String.format("결제 금액이 일치하지 않습니다. 주문금액: %d, 결제금액: %d", 
                            order.getTotalAmount(), request.getAmount()));
        }

        // 2. 이미 해당 결제키로 처리가 완료되었는지 확인
        Optional<Payment> existingPaymentByKey = paymentRepository.findByPaymentKey(request.getPaymentKey());
        
        if (existingPaymentByKey.isPresent()) {
            Payment existingPayment = existingPaymentByKey.get();
            log.info("이미 처리된 결제입니다 - paymentId: {}, orderId: {}, paymentKey: {}", 
                    existingPayment.getId(), existingPayment.getOrder().getId(), request.getPaymentKey());
            
            // 기존 결제 정보 반환
            Map<String, Object> result = new HashMap<>();
            result.put("paymentId", existingPayment.getId());
            result.put("orderId", existingPayment.getOrder().getId());
            result.put("amount", existingPayment.getAmount());
            result.put("paymentKey", existingPayment.getPaymentKey());
            result.put("status", existingPayment.getPaymentStatus().name());
            log.info("기존 결제 정보 반환 - paymentKey: {}", existingPayment.getPaymentKey());
            return result;
        }
        
        // 3. 같은 주문에 이미 결제가 있는지 확인
        Optional<Payment> existingPaymentOpt = paymentRepository.findByOrderId(order.getId());
        
        if (existingPaymentOpt.isPresent()) {
            Payment existingPayment = existingPaymentOpt.get();
            log.warn("이미 주문에 결제가 존재합니다 - paymentId: {}, orderId: {}", 
                    existingPayment.getId(), order.getId());
            
            // 기존 결제 정보 반환
            Map<String, Object> result = new HashMap<>();
            result.put("paymentId", existingPayment.getId());
            result.put("orderId", order.getId());
            result.put("amount", existingPayment.getAmount());
            result.put("paymentKey", existingPayment.getPaymentKey());
            result.put("status", existingPayment.getPaymentStatus().name());
            return result;
        }
        
        log.info("새로운 결제 승인 처리 시작");

        // 4. 토스페이먼츠 API 호출
        // 프론트엔드에서 전달한 tossOrderId가 있으면 사용, 없으면 Order의 getTossOrderId() 사용
        String tossOrderId = (request.getTossOrderId() != null && !request.getTossOrderId().isEmpty()) 
                ? request.getTossOrderId()  // 프론트엔드에서 전달한 타임스탬프 포함 원본
                : order.getTossOrderId();   // 기존 방식 (타임스탬프 없음)
        
        log.info("토스페이먼츠 orderId 사용 - tossOrderId: {}", tossOrderId);
        
        JSONObject responseData;
        try {
            responseData = callTossPaymentsApi(request, tossOrderId);
        } catch (Exception e) {
            // 결제 실패 시 주문 취소 및 재고 복구
            log.error("토스페이먼츠 API 호출 실패 - 주문 취소 및 재고 복구 시작 - orderId: {}", order.getId(), e);
            try {
                orderService.cancelOrder(order.getId(), "결제 실패로 인한 자동 취소");
                log.info("결제 실패로 인한 주문 취소 완료 - orderId: {}", order.getId());
            } catch (Exception cancelException) {
                log.error("주문 취소 중 오류 발생 - orderId: {}", order.getId(), cancelException);
            }
            throw e;
        }

        // 5. Payment 엔티티 저장
        Payment payment = Payment.builder()
                .order(order)
                .amount(request.getAmount())
                .build();

        payment.confirm(request.getPaymentKey(), (String) responseData.get("transactionKey"));
        paymentRepository.save(payment);

        // 결제 완료 후 주문 상태는 PENDING 유지 (관리자 승인 대기)
        // 관리자가 approveOrder를 호출할 때 CONFIRMED로 변경됨
        log.info("결제 승인 완료 - paymentId: {}, orderId: {}, 주문 상태: {} (관리자 승인 대기)", 
                payment.getId(), order.getId(), order.getOrderStatus());

        // 6. 응답 데이터 변환
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
     * @param request 결제 확인 요청 (프론트에서 받은 데이터)
     * @param tossOrderId 토스페이먼츠용 orderId (CAREUP_ORDER_형식)
     */
    @SuppressWarnings("unchecked")
    private JSONObject callTossPaymentsApi(PaymentConfirmRequest request, String tossOrderId) throws Exception {
        // 요청 데이터 생성 -  tossOrderId 사용
        JSONObject requestData = new JSONObject();
        requestData.put("orderId", tossOrderId);  //  getTossOrderId() 결과 사용
        requestData.put("amount", request.getAmount());
        requestData.put("paymentKey", request.getPaymentKey());

        log.info("토스페이먼츠 API 호출 - tossOrderId: {}, amount: {}", tossOrderId, request.getAmount());

        // Authorization 헤더 생성  Config 사용
        String authorization = "Basic " + tossPaymentConfig.getEncodedSecretKey();

        // HTTP 연결 설정  Config의 API URL 사용
        URL url = new URL(tossPaymentConfig.getApiUrl() + "/v1/payments/confirm");
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
