package com.careup.branch.domain.purchaseOrder.controller;

import com.careup.branch.domain.purchaseOrder.dto.PartialApproveRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderListResponseDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.careup.branch.domain.purchaseOrder.service.PurchaseOrderExcelService;
import com.careup.branch.domain.purchaseOrder.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {
    
    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderExcelService excelService;

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
    public ResponseEntity<PurchaseOrderResponseDto> partialApprovePurchaseOrder(
            @PathVariable Long purchaseOrderId, 
            @RequestBody PartialApproveRequestDto requestDto) {
        PurchaseOrderResponseDto partialApprovedOrder = purchaseOrderService.partialApprovePurchaseOrder(purchaseOrderId, requestDto);
        return ResponseEntity.ok(partialApprovedOrder);
    }

    // 발주 배송 시작 (본사용)
    @PostMapping("/{purchaseOrderId}/ship")
    public ResponseEntity<PurchaseOrderResponseDto> shipPurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto shippedOrder = purchaseOrderService.shipPurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(shippedOrder);
    }

    // 발주 입고 완료 (가맹점용)
    @PostMapping("/{purchaseOrderId}/complete")
    public ResponseEntity<PurchaseOrderResponseDto> completePurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto completedOrder = purchaseOrderService.completePurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(completedOrder);
    }


    // 발주 내역 엑셀 다운로드 (본사/가맹점)
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam Long branchId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        byte[] excelBytes = excelService.exportToExcel(branchId, startDate, endDate);
        
        // 파일명 생성
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "purchase_orders_" + timestamp + ".xlsx";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelBytes.length);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }

}
