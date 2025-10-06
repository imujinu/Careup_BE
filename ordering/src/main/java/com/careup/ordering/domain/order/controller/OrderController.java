package com.careup.ordering.domain.order.controller;

import com.careup.ordering.common.dto.ApiResponseDto;
import com.careup.ordering.domain.order.dto.OrderResponseDto;
import com.careup.ordering.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/member/{memberId}")
    private ResponseEntity<ApiResponseDto<List<OrderResponseDto>>> getOrder
}
