package com.careup.ordering.domain.payment.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.order.service.OrderService;
import com.careup.ordering.domain.payment.dto.PaymentConfirmRequest;
import com.careup.ordering.domain.payment.entity.Payment;
import com.careup.ordering.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;

    /**
     * 결제 승인
     */
    @PostMapping("/confirm")
    public ResponseEntity<ResponseDto<Map<String, Object>>> confirmPayment(
            @RequestBody PaymentConfirmRequest request) {
        
        log.info("POST /api/payments/confirm - 결제 승인 요청");
        log.info("Request - orderId: {}, tossOrderId: {}, paymentKey: {}, amount: {}", 
                request.getOrderId(), request.getTossOrderId(), request.getPaymentKey(), request.getAmount());

        try {
            Map<String, Object> response = paymentService.confirmPayment(request);

            return new ResponseEntity<>(
                    ResponseDto.ok(response, HttpStatus.OK), 
                    HttpStatus.OK
            );
        } catch (IllegalArgumentException e) {
            log.error("결제 승인 실패 - 잘못된 요청: {}", e.getMessage());
            return new ResponseEntity<>(
                    ResponseDto.fail(HttpStatus.BAD_REQUEST, e.getMessage()), 
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("결제 승인 실패 - 서버 오류", e);
            return new ResponseEntity<>(
                    ResponseDto.fail(HttpStatus.INTERNAL_SERVER_ERROR, 
                            "결제 처리 중 오류가 발생했습니다: " + e.getMessage()), 
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * 결제 성공 페이지 (프론트엔드에서 처리하므로 필요시 사용)
     */
    @GetMapping("/success")
    public ResponseEntity<ResponseDto<String>> success() {
        return new ResponseEntity<>(
                ResponseDto.ok("결제 성공 페이지", HttpStatus.OK), 
                HttpStatus.OK
        );
    }

    /**
     * 결제 실패 페이지 (프론트엔드에서 처리하므로 필요시 사용)
     * orderId가 제공되면 주문 취소 처리도 수행
     */
    @GetMapping("/fail")
    public ResponseEntity<ResponseDto<Map<String, String>>> fail(
            @RequestParam String code,
            @RequestParam String message,
            @RequestParam(required = false) String orderId) {
        
        log.error("결제 실패 - code: {}, message: {}, orderId: {}", code, message, orderId);

        // orderId가 제공되고 CAREUP_ORDER_X 형식인 경우 주문 취소 처리
        if (orderId != null && !orderId.isEmpty()) {
            try {
                // CAREUP_ORDER_X 또는 CAREUP_ORDER_X_timestamp 형식에서 실제 주문 ID 추출
                String numericOrderIdStr = null;
                if (orderId.startsWith("CAREUP_ORDER_")) {
                    String[] parts = orderId.replace("CAREUP_ORDER_", "").split("_");
                    numericOrderIdStr = parts[0];
                } else {
                    numericOrderIdStr = orderId;
                }

                if (numericOrderIdStr != null) {
                    Long numericOrderId = Long.parseLong(numericOrderIdStr);
                    log.info("결제 실패로 인한 주문 취소 처리 시작 - orderId: {}", numericOrderId);
                    
                    try {
                        orderService.cancelOrder(numericOrderId, "결제 실패로 인한 자동 취소");
                        log.info("주문 취소 완료 - orderId: {}", numericOrderId);
                    } catch (IllegalStateException e) {
                        log.warn("주문 취소 실패 (이미 승인됨 등) - orderId: {}, error: {}", numericOrderId, e.getMessage());
                    } catch (IllegalArgumentException e) {
                        log.warn("주문 취소 실패 (주문 없음) - orderId: {}, error: {}", numericOrderId, e.getMessage());
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("주문 ID 파싱 실패 - orderId: {}", orderId);
            } catch (Exception e) {
                log.error("결제 실패 시 주문 취소 처리 중 오류 발생", e);
                // 주문 취소 실패해도 응답은 정상 반환 (결제 실패는 이미 발생했으므로)
            }
        }

        Map<String, String> failInfo = new HashMap<>();
        failInfo.put("code", code);
        failInfo.put("message", message);
        if (orderId != null) {
            failInfo.put("orderId", orderId);
        }

        return new ResponseEntity<>(
                ResponseDto.ok(failInfo, HttpStatus.OK), 
                HttpStatus.OK
        );
    }
    // 결제 단건 조회
    @GetMapping("/{paymentId}")
    public ResponseEntity<ResponseDto<Payment>> getPayment(@PathVariable Long paymentId) {
        Payment payment = paymentService.getPaymentById(paymentId);
        return new ResponseEntity<>(ResponseDto.ok(payment, HttpStatus.OK), HttpStatus.OK);
    }

    // 주문별 결제 조회
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ResponseDto<Payment>> getPaymentByOrder(@PathVariable Long orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        return new ResponseEntity<>(ResponseDto.ok(payment, HttpStatus.OK), HttpStatus.OK);
    }

    // 회원별 결제 목록 조회
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ResponseDto<List<Payment>>> getPaymentsByMember(@PathVariable Long memberId) {
        List<Payment> payments = paymentService.getPaymentsByMemberId(memberId);
        return new ResponseEntity<>(ResponseDto.ok(payments, HttpStatus.OK), HttpStatus.OK);
    }

    // 결제 삭제
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<ResponseDto<Void>> deletePayment(@PathVariable Long paymentId) {
        paymentService.deletePayment(paymentId);
        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
}
