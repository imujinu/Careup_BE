package com.careup.branch.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "ordering-service", url = "${feign.ordering.url:http://localhost:8080}")
public interface OrderingInventoryClient {

     // 상품 목록 조회
    @GetMapping("/api/products")
    List<ProductResponseDto> getProducts();

     // 상품 상세 조회
    @GetMapping("/api/products/{productId}")
    ProductResponseDto getProduct(@PathVariable Long productId);

     // 상품 등록
    @PostMapping("/api/products")
    ProductResponseDto createProduct(@RequestBody ProductRequestDto request);

     // 상품 수정
    @PutMapping("/api/products/{productId}")
    ProductResponseDto updateProduct(@PathVariable Long productId, @RequestBody ProductRequestDto request);

     // 상품 삭제
    @DeleteMapping("/api/products/{productId}")
    void deleteProduct(@PathVariable Long productId);

    // 재고 관리

     // 지점별 상품 재고 조회
    @GetMapping("/api/inventory/branch/{branchId}")
    List<BranchProductResponseDto> getBranchProducts(@PathVariable Long branchId);

     // 특정 상품의 지점별 재고 조회
    @GetMapping("/api/inventory/branch/{branchId}/product/{productId}")
    BranchProductResponseDto getBranchProduct(@PathVariable Long branchId, @PathVariable Long productId);

     // 안전재고 설정
    @PostMapping("/api/inventory/safety-stock")
    void updateSafetyStock(@RequestBody SafetyStockRequest request);

     // 재고 증감
    @PostMapping("/api/inventory/adjust")
    void adjustStock(@RequestBody StockAdjustRequest request);

    // 입출고 관리

     // 입출고 조회
    @GetMapping("/api/inventory/flow")
    List<InventoryFlowResponseDto> getInventoryFlows(@RequestParam(required = false) Long branchId,
                                                     @RequestParam(required = false) Long productId);

     // 입출고 등록
    @PostMapping("/api/inventory/flow")
    InventoryFlowResponseDto createInventoryFlow(@RequestBody InventoryFlowRequest request);

     // 입출고 수정
    @PutMapping("/api/inventory/flow/{flowId}")
    InventoryFlowResponseDto updateInventoryFlow(@PathVariable Long flowId, @RequestBody InventoryFlowRequest request);

     // 입출고 기록 삭제
    @DeleteMapping("/api/inventory/flow/{flowId}")
    void deleteInventoryFlow(@PathVariable Long flowId);

     // 재고 조정 내역 조회 (불량/폐기)
    @GetMapping("/api/inventory/adjustment-history")
    List<InventoryFlowResponseDto> getAdjustmentHistory(@RequestParam(required = false) Long branchId,
                                                        @RequestParam(required = false) String reason);

    // dto
    
    // Product
    public static class ProductRequestDto {
        public Long categoryId;
        public String name;
        public String description;
        public Long supplyPrice;
        public Long minPrice;
        public Long maxPrice;
        public String imageUrl;
    }
    
    public static class ProductResponseDto {
        public Long productId;
        public Long categoryId;
        public String name;
        public String description;
        public Long supplyPrice;
        public Long minPrice;
        public Long maxPrice;
        public String imageUrl;
        public String status;
        public String visibility;
    }
    
    // BranchProduct
    public static class BranchProductResponseDto {
        public Long branchProductId;
        public Long productId;
        public Long branchId;
        public String serialNumber;
        public Long stockQuantity;
        public Long safetyStock;
        public Long price;
        public String productName;
        public String productDescription;
    }
    
    // 안전재고 설정
    public static class SafetyStockRequest {
        public Long branchProductId;
        public Long safetyStock;
    }
    
    // 재고 조절
    public static class StockAdjustRequest {
        public Long branchProductId;
        public Long quantity;
        public String type; // "INCREASE" or "DECREASE"
        public String reason;
    }
    
    // 입출고 기록
    public static class InventoryFlowRequest {
        public Long branchProductId;
        public Long inQuantity;
        public Long outQuantity;
        public String remark;
    }
    
    public static class InventoryFlowResponseDto {
        public Long flowId;
        public Long branchProductId;
        public Long productId;
        public String productName;
        public Long branchId;
        public Long inQuantity;
        public Long outQuantity;
        public String remark;
        public String createdAt;
    }
}
