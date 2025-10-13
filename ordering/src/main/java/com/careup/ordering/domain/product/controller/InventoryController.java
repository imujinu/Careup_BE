package com.careup.ordering.domain.product.controller;

import com.careup.ordering.domain.product.dto.BranchProductRequestDto;
import com.careup.ordering.domain.product.dto.BranchProductResponseDto;
import com.careup.ordering.domain.product.dto.InventoryFlowRequestDto;
import com.careup.ordering.domain.product.dto.InventoryFlowResponseDto;
import com.careup.ordering.domain.product.dto.SafetyStockRequestDto;
import com.careup.ordering.domain.product.dto.StockAdjustRequestDto;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.InventoryFlowDetail;
import com.careup.ordering.domain.product.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryService inventoryService;

    // ==================== 재고 관리 ====================

    // 지점별 상품 등록 (BranchProduct 생성)
    @PostMapping("/branch-products")
    public ResponseEntity<BranchProductResponseDto> createBranchProduct(@RequestBody BranchProductRequestDto request) {
        BranchProduct branchProduct = inventoryService.createBranchProduct(
                request.getProductId(),
                request.getBranchId(),
                request.getSerialNumber(),
                request.getStockQuantity(),
                request.getSafetyStock(),
                request.getPrice()
        );
        return ResponseEntity.ok(convertToBranchProductResponse(branchProduct));
    }
    
    // 지점별 재고 조회
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<BranchProductResponseDto>> getBranchProducts(@PathVariable Long branchId) {
        List<BranchProduct> branchProducts = inventoryService.getBranchProducts(branchId);
        List<BranchProductResponseDto> response = branchProducts.stream()
                .map(this::convertToBranchProductResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 특정 상품의 지점별 재고 조회
    @GetMapping("/branch/{branchId}/product/{productId}")
    public ResponseEntity<BranchProductResponseDto> getBranchProduct(
            @PathVariable Long branchId,
            @PathVariable Long productId) {
        BranchProduct branchProduct = inventoryService.getBranchProduct(branchId, productId);
        return ResponseEntity.ok(convertToBranchProductResponse(branchProduct));
    }

    // 안전재고 설정
    @PostMapping("/safety-stock")
    public ResponseEntity<Void> updateSafetyStock(@RequestBody SafetyStockRequestDto request) {
        inventoryService.updateSafetyStock(request.getBranchProductId(), request.getSafetyStock());
        return ResponseEntity.ok().build();
    }

    // 재고 증감
    @PostMapping("/adjust")
    public ResponseEntity<Void> adjustStock(@RequestBody StockAdjustRequestDto request) {
        inventoryService.adjustStock(
                request.getBranchProductId(),
                request.getQuantity(),
                request.getType(),
                request.getReason()
        );
        return ResponseEntity.ok().build();
    }
    
    // ==================== 입출고 관리 ====================
    
    // 입출고 기록 조회
    @GetMapping("/flow")
    public ResponseEntity<List<InventoryFlowResponseDto>> getInventoryFlows(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long productId) {
        
        List<InventoryFlowDetail> flows = inventoryService.getInventoryFlows(branchId, productId);
        List<InventoryFlowResponseDto> response = flows.stream()
                .map(this::convertToInventoryFlowResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    // 입출고 기록 등록
    @PostMapping("/flow")
    public ResponseEntity<InventoryFlowResponseDto> createInventoryFlow(@RequestBody InventoryFlowRequestDto request) {
        InventoryFlowDetail flow = inventoryService.createInventoryFlow(
                request.getBranchProductId(),
                request.getInQuantity(),
                request.getOutQuantity(),
                request.getRemark()
        );
        
        return ResponseEntity.ok(convertToInventoryFlowResponse(flow));
    }

    // 입출고 기록 삭제
    @DeleteMapping("/flow/{flowId}")
    public ResponseEntity<Void> deleteInventoryFlow(@PathVariable Long flowId) {
        inventoryService.deleteInventoryFlow(flowId);
        return ResponseEntity.ok().build();
    }

    // 재고 조절 내역 조회 (불량/폐기 포함)
    @GetMapping("/adjustment-history")
    public ResponseEntity<List<InventoryFlowResponseDto>> getAdjustmentHistory(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String reason) {
        
        List<InventoryFlowDetail> flows = inventoryService.getAdjustmentHistory(branchId, reason);
        List<InventoryFlowResponseDto> response = flows.stream()
                .map(this::convertToInventoryFlowResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== DTO 변환 메서드 ====================
    
    private BranchProductResponseDto convertToBranchProductResponse(BranchProduct branchProduct) {
        return new BranchProductResponseDto(
                branchProduct.getId(),
                branchProduct.getProduct().getId(),
                branchProduct.getBranchId(),
                branchProduct.getSerialNumber(),
                branchProduct.getStockQuantity(),
                branchProduct.getSafetystock(),
                branchProduct.getPrice(),
                branchProduct.getProduct().getName(),
                branchProduct.getProduct().getDescription()
        );
    }

    private InventoryFlowResponseDto convertToInventoryFlowResponse(InventoryFlowDetail flow) {
        return new InventoryFlowResponseDto(
                flow.getId(),
                flow.getBranchProduct().getId(),
                flow.getBranchProduct().getProduct().getId(),
                flow.getBranchProduct().getProduct().getName(),
                flow.getBranchProduct().getBranchId(),
                flow.getInQuantity(),
                flow.getOutQuantity(),
                flow.getRemark(),
                flow.getCreateAt().toString()
        );
    }
}