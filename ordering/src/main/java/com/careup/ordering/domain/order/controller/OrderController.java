package com.careup.ordering.domain.order.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.order.dto.OrderRequestDto;
import com.careup.ordering.domain.order.dto.OrderResponseDto;
import com.careup.ordering.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ResponseDto<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderRequestDto requestDto) {

        OrderResponseDto response = orderService.createOrder(requestDto);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ResponseDto<OrderResponseDto>> getOrder(
            @PathVariable Long orderId) {

        OrderResponseDto response = orderService.getOrderById(orderId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ResponseDto<List<OrderResponseDto>>> getOrdersByMember(
            @PathVariable Long memberId) {

        List<OrderResponseDto> response = orderService.getOrdersByMember(memberId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ResponseDto<List<OrderResponseDto>>> getOrdersByBranch(
            @PathVariable Long branchId) {

        List<OrderResponseDto> response = orderService.getOrdersByBranch(branchId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    @PutMapping("/{orderId}/approve")
    public ResponseEntity<ResponseDto<OrderResponseDto>> approveOrder(
            @PathVariable Long orderId,
            @RequestParam Long approvedBy) {

        OrderResponseDto response = orderService.approveOrder(orderId, approvedBy);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    @PutMapping("/{orderId}/reject")
    public ResponseEntity<ResponseDto<OrderResponseDto>> rejectOrder(
            @PathVariable Long orderId,
            @RequestParam String reason) {

        OrderResponseDto response = orderService.rejectOrder(orderId, reason);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ResponseDto<Void>> cancelOrder(
            @PathVariable Long orderId) {

        orderService.cancelOrder(orderId);

        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
}