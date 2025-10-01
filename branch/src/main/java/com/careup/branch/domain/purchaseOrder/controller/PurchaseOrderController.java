package com.careup.branch.domain.purchaseOrder.controller;

import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderListResponseDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.careup.branch.domain.purchaseOrder.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {
    
    private final PurchaseOrderService purchaseOrderService;

     // 발주 생성 (가맹점용)
    @PostMapping
    public ResponseEntity<PurchaseOrderResponseDto> createPurchaseOrder(@RequestBody PurchaseOrderRequestDto requestDto) {
        PurchaseOrderResponseDto createdOrder = purchaseOrderService.createPurchaseOrder(requestDto);
        return ResponseEntity.ok(createdOrder);
    }

    // 발주 목록 조회 (본사/가맹점 구분)
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<PurchaseOrderListResponseDto>> getPurchaseOrders(@PathVariable Long branchId) {
        List<PurchaseOrderListResponseDto> purchaseOrders = purchaseOrderService.getPurchaseOrders(branchId);
        return ResponseEntity.ok(purchaseOrders);
    }
    

     // 발주 상세 조회
    @GetMapping("/{purchaseOrderId}")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto purchaseOrder = purchaseOrderService.getPurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(purchaseOrder);
    }

    // 발주 승인 (본사용)
    @PostMapping("/{purchaseOrderId}/approve")
    public ResponseEntity<PurchaseOrderResponseDto> approvePurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto approvedOrder = purchaseOrderService.approvePurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(approvedOrder);
    }

    // 발주 반려 (본사용)
    @PostMapping("/{purchaseOrderId}/reject")
    public ResponseEntity<PurchaseOrderResponseDto> rejectPurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto rejectedOrder = purchaseOrderService.rejectPurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(rejectedOrder);
    }

    // 발주 부분 승인 (본사용)
    @PostMapping("/{purchaseOrderId}/partial-approve")
    public ResponseEntity<PurchaseOrderResponseDto> partialApprovePurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto partialApprovedOrder = purchaseOrderService.partialApprovePurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(partialApprovedOrder);
    }

}
