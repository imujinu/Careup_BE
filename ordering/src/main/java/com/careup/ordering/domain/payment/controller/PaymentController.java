package com.careup.ordering.domain.payment.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.payment.dto.PaymentConfirmRequest;
import com.careup.ordering.domain.payment.entity.Payment;
import com.careup.ordering.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

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
     */
    @GetMapping("/fail")
    public ResponseEntity<ResponseDto<Map<String, String>>> fail(
            @RequestParam String code,
            @RequestParam String message) {
        
        log.error("결제 실패 - code: {}, message: {}", code, message);

        Map<String, String> failInfo = Map.of(
                "code", code,
                "message", message
        );

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
