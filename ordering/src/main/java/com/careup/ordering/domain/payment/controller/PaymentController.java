package com.careup.ordering.domain.payment.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.payment.dto.PaymentConfirmRequest;
import com.careup.ordering.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        try {
            Map<String, Object> response = paymentService.confirmPayment(request);

            return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
        } catch (Exception e) {
            log.error("결제 승인 실패", e);
            return new ResponseEntity<>(ResponseDto.fail(HttpStatus.BAD_REQUEST, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
}