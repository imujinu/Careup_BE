package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.BranchProductRequestDto;
import com.careup.ordering.domain.product.dto.BranchProductResponseDto;
import com.careup.ordering.domain.product.dto.InventoryFlowRequestDto;
import com.careup.ordering.domain.product.dto.InventoryFlowResponseDto;
import com.careup.ordering.domain.product.dto.InventoryUpdateRequestDto;
import com.careup.ordering.domain.product.dto.SafetyStockRequestDto;
import com.careup.ordering.domain.product.dto.StockAdjustRequestDto;
import com.careup.ordering.domain.product.dto.ReservationRequestDto;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.InventoryFlowDetail;
import com.careup.ordering.domain.product.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 재고 및 지점 상품 통합 컨트롤러
 * - 재고 관리 (등록/조회/수정/입출고)
 * - 지점 상품 조회 (프로모션 포함)
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {
    
    private final InventoryService inventoryService;

    // ==================== 지점 상품 조회 (프로모션 포함)  ====================
    
    /**
     *  전체 지점 상품 조회 (프로모션 포함)
     */
    @GetMapping("/branch-products")
    public ResponseEntity<ResponseDto> getAllBranchProductsWithPromotion() {
        List<BranchProductResponseDto> products = inventoryService.getAllBranchProductsWithPromotion();
        return new ResponseEntity<>(ResponseDto.ok(products, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     *  지점별 상품 조회 (프로모션 포함)
     */
    @GetMapping("/branch-products/branch/{branchId}")
    public ResponseEntity<ResponseDto> getBranchProductsByBranchWithPromotion(@PathVariable Long branchId) {
        List<BranchProductResponseDto> products = inventoryService.getBranchProductsByBranchWithPromotion(branchId);
        return new ResponseEntity<>(ResponseDto.ok(products, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     *  지점 상품 상세 조회 (프로모션 포함)
     */
    @GetMapping("/branch-products/{branchProductId}")
    public ResponseEntity<ResponseDto> getBranchProductWithPromotion(@PathVariable Long branchProductId) {
        BranchProductResponseDto product = inventoryService.getBranchProductWithPromotion(branchProductId);
        return new ResponseEntity<>(ResponseDto.ok(product, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     *  상품명 검색 (프로모션 포함)
     */
    @GetMapping("/branch-products/search")
    public ResponseEntity<ResponseDto> searchBranchProductsWithPromotion(@RequestParam String keyword) {
        List<BranchProductResponseDto> products = inventoryService.searchBranchProductsWithPromotion(keyword);
        return new ResponseEntity<>(ResponseDto.ok(products, HttpStatus.OK), HttpStatus.OK);
    }
    
    /**
     *  지점별 + 상품명 검색 (프로모션 포함)
     */
    @GetMapping("/branch-products/branch/{branchId}/search")
    public ResponseEntity<ResponseDto> searchBranchProductsByBranchWithPromotion(
            @PathVariable Long branchId,
            @RequestParam String keyword) {
        List<BranchProductResponseDto> products = 
                inventoryService.searchBranchProductsByBranchWithPromotion(branchId, keyword);
        return new ResponseEntity<>(ResponseDto.ok(products, HttpStatus.OK), HttpStatus.OK);
    }

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
                request.getPrice(),
                request.getAttributeValueId()  // 사이즈별 재고 관리를 위한 속성 값 ID
        );
        return ResponseEntity.ok(convertToBranchProductResponse(branchProduct));
    }
    
    // 지점별 재고 조회 (재고 관리용 - 프로모션 미포함)
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<BranchProductResponseDto>> getBranchProducts(
            @PathVariable Long branchId, 
            Authentication authentication) {
        // 자동발주 페이지에서 본사 조회 허용
        Long actualBranchId;
        if (branchId == 1L) {
            // 본사 조회는 자동발주 등에서 사용하므로 허용
            actualBranchId = 1L;
        } else {
            // 사용자는 자신의 지점 재고만 조회 가능
            actualBranchId = inventoryService.getCurrentUserBranchId(authentication);
        }

        List<BranchProduct> branchProducts = inventoryService.getBranchProducts(actualBranchId, authentication);
        List<BranchProductResponseDto> response = branchProducts.stream()
                .map(this::convertToBranchProductResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 특정 상품의 지점별 재고 조회
    @GetMapping("/branch/{branchId}/product/{productId}")
    public ResponseEntity<BranchProductResponseDto> getBranchProduct(
            @PathVariable Long branchId,
            @PathVariable Long productId,
            Authentication authentication) {
        BranchProduct branchProduct = inventoryService.getBranchProduct(branchId, productId, authentication);
        return ResponseEntity.ok(convertToBranchProductResponse(branchProduct));
    }

    // 안전재고 설정
    @PostMapping("/safety-stock")
    public ResponseEntity<Void> updateSafetyStock(@RequestBody SafetyStockRequestDto request, Authentication authentication) {
        inventoryService.updateSafetyStock(request.getBranchProductId(), request.getSafetyStock(), authentication);
        return ResponseEntity.ok().build();
    }

    // 재고 정보 수정 (안전재고, 단가)
    @PostMapping("/update")
    public ResponseEntity<Void> updateInventoryInfo(@RequestBody InventoryUpdateRequestDto request, Authentication authentication) {
        inventoryService.updateInventoryInfo(request.getBranchProductId(), request.getSafetyStock(), request.getUnitPrice(), authentication);
        return ResponseEntity.ok().build();
    }

    // 지점 상품 삭제
    @DeleteMapping("/branch-products/{branchProductId}")
    public ResponseEntity<Void> deleteBranchProduct(@PathVariable Long branchProductId, Authentication authentication) {
        inventoryService.deleteBranchProduct(branchProductId, authentication);
        return ResponseEntity.ok().build();
    }

    // 재고 증감
    @PostMapping("/adjust")
    public ResponseEntity<Void> adjustStock(@RequestBody StockAdjustRequestDto request, Authentication authentication) {
        inventoryService.adjustStock(
                request.getBranchProductId(),
                request.getQuantity(),
                request.getType(),
                request.getReason(),
                authentication
        );
        return ResponseEntity.ok().build();
    }

    // ========== 예약 재고 ==========
    @PostMapping("/reserve")
    public ResponseEntity<Void> reserve(@RequestBody ReservationRequestDto request) {
        inventoryService.reserveStock(request.getBranchProductId(), request.getQuantity(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reservation/release")
    public ResponseEntity<Void> releaseReservation(@RequestBody ReservationRequestDto request) {
        inventoryService.releaseReservation(request.getBranchProductId(), request.getQuantity(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reservation/commit")
    public ResponseEntity<Void> commitReservation(@RequestBody ReservationRequestDto request) {
        inventoryService.commitReservation(request.getBranchProductId(), request.getQuantity(), request.getReason());
        return ResponseEntity.ok().build();
    }

    // ==================== 입출고 관리 ====================
    
    // 입출고 기록 조회
    @GetMapping("/flow")
    public ResponseEntity<List<InventoryFlowResponseDto>> getInventoryFlows(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Long productId,
            Authentication authentication) {
        
        List<InventoryFlowDetail> flows = inventoryService.getInventoryFlows(branchId, productId, authentication);
        List<InventoryFlowResponseDto> response = flows.stream()
                .map(this::convertToInventoryFlowResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    // 입출고 기록 등록
    @PostMapping("/flow")
    public ResponseEntity<InventoryFlowResponseDto> createInventoryFlow(@RequestBody InventoryFlowRequestDto request, Authentication authentication) {
        InventoryFlowDetail flow = inventoryService.createInventoryFlow(
                request.getBranchProductId(),
                request.getInQuantity(),
                request.getOutQuantity(),
                request.getRemark(),
                authentication
        );
        
        return ResponseEntity.ok(convertToInventoryFlowResponse(flow));
    }

    // 입출고 기록 수정
    @PutMapping("/flow/{flowId}")
    public ResponseEntity<InventoryFlowResponseDto> updateInventoryFlow(
            @PathVariable Long flowId,
            @RequestBody InventoryFlowRequestDto request,
            Authentication authentication) {
        InventoryFlowDetail flow = inventoryService.updateInventoryFlow(
                flowId,
                request.getInQuantity(),
                request.getOutQuantity(),
                request.getRemark(),
                authentication
        );
        
        return ResponseEntity.ok(convertToInventoryFlowResponse(flow));
    }

    // 입출고 기록 삭제
    @DeleteMapping("/flow/{flowId}")
    public ResponseEntity<Void> deleteInventoryFlow(@PathVariable Long flowId, Authentication authentication) {
        inventoryService.deleteInventoryFlow(flowId, authentication);
        return ResponseEntity.ok().build();
    }

    // 재고 조절 내역 조회 (불량/폐기 포함)
    @GetMapping("/adjustment-history")
    public ResponseEntity<List<InventoryFlowResponseDto>> getAdjustmentHistory(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        
        List<InventoryFlowDetail> flows = inventoryService.getAdjustmentHistory(branchId, reason, authentication);
        List<InventoryFlowResponseDto> response = flows.stream()
                .map(this::convertToInventoryFlowResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    // ==================== DTO 변환 메서드 ====================
    
    private BranchProductResponseDto convertToBranchProductResponse(BranchProduct branchProduct) {
        return BranchProductResponseDto.from(branchProduct);
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