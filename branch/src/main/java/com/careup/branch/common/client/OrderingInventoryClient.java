package com.careup.branch.common.client;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.chat.dto.stock.StockAdjustRequestDto;
import lombok.Builder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "ordering-service",
        url = "${feign.ordering.url:http://localhost:8080}",
        configuration = com.careup.branch.common.config.FeignConfig.class
)
public interface OrderingInventoryClient {

    // ====== 상품 조회 (Branch에서 실제 사용 중) ======
    @GetMapping("/api/products/{productId}")
    ResponseDto<ProductResponseDto> getProduct(@PathVariable Long productId);

    // 상품 속성 값 조회
    @GetMapping("/api/product-attribute-values/product/{productId}")
    ResponseDto<List<ProductAttributeValueResponseDto>> getProductAttributeValues(@PathVariable Long productId);

    // ====== 필요 시 사용 가능한 나머지 API (그대로 두셔도 OK) ======
    @GetMapping("/api/products")
    List<ProductResponseDto> getProducts();

    @PostMapping("/api/products")
    ProductResponseDto createProduct(@RequestBody ProductRequestDto request);

    @PutMapping("/api/products/{productId}")
    ProductResponseDto updateProduct(@PathVariable Long productId, @RequestBody ProductRequestDto request);

    @DeleteMapping("/api/products/{productId}")
    void deleteProduct(@PathVariable Long productId);

    @GetMapping("/inventory/branch/{branchId}")
    List<BranchProductResponseDto> getBranchProducts(@PathVariable Long branchId);

    @GetMapping("/inventory/branch/{branchId}/product/{productId}")
    BranchProductResponseDto getBranchProduct(@PathVariable Long branchId, @PathVariable Long productId);

    @PostMapping("/inventory/safety-stock")
    void updateSafetyStock(@RequestBody SafetyStockRequest request);

    @PostMapping("/inventory/adjust")
    void adjustStock(@RequestBody StockAdjustRequest request);

    // 예약재고
    @PostMapping("/inventory/reserve")
    void reserve(@RequestBody ReservationRequest request);

    @PostMapping("/inventory/reservation/release")
    void releaseReservation(@RequestBody ReservationRequest request);

    @PostMapping("/inventory/reservation/commit")
    void commitReservation(@RequestBody ReservationRequest request);

    @GetMapping("/inventory/flow")
    List<InventoryFlowResponseDto> getInventoryFlows(@RequestParam(required = false) Long branchId,
                                                     @RequestParam(required = false) Long productId);

    @PostMapping("/inventory/flow")
    InventoryFlowResponseDto createInventoryFlow(@RequestBody InventoryFlowRequest request);

    @PutMapping("/inventory/flow/{flowId}")
    InventoryFlowResponseDto updateInventoryFlow(@PathVariable Long flowId, @RequestBody InventoryFlowRequest request);

    @DeleteMapping("/inventory/flow/{flowId}")
    void deleteInventoryFlow(@PathVariable Long flowId);

    @GetMapping("/inventory/adjustment-history")
    List<InventoryFlowResponseDto> getAdjustmentHistory(@RequestParam(required = false) Long branchId,
                                                        @RequestParam(required = false) String reason);


    // ====== 응답 래퍼 (Ordering의 ResponseDto와 호환: data 필드만 사용) ======
    class ResponseDto<T> {
        public T data;

        public T data() {
            return data;
        }
    }

    // ====== DTOs ======

    // Product
    class ProductRequestDto {
        public Long categoryId;
        public String name;
        public String description;
        public Long supplyPrice;
        public Long minPrice;
        public Long maxPrice;
        public String imageUrl;
    }

    class ProductResponseDto {
        public Long productId;
        public Long categoryId;
        public String categoryName;
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
    class BranchProductResponseDto {
        public Long branchProductId;
        public Long productId;
        public Long branchId;
        public String serialNumber;
        public Long stockQuantity;
        public Long reservedQuantity;  // 예약재고 수량
        public Long availableQuantity;  // 사용 가능한 재고 수량
        public Long safetyStock;
        public Long price;
        public String productName;
        public String productDescription;
        public Long attributeValueId;
        public String attributeValueName;
        public String attributeTypeName;
    }

    // 안전재고 설정
    class SafetyStockRequest {
        public Long branchProductId;
        public Long safetyStock;
    }

    // 재고 조절
    @Builder
    class StockAdjustRequest {
        public Long branchProductId;
        public Long quantity;
        public String type; // "INCREASE" or "DECREASE"
        public String reason;

        public static StockAdjustRequest makeDto(Long productId, Long quantity, String type, String reason){
            return StockAdjustRequest.builder()
                    .branchProductId(productId)
                    .quantity(quantity)
                    .type(type)
                    .reason(reason)
                    .build();
        }
    }

    // 입출고 기록
    class InventoryFlowRequest {
        public Long branchProductId;
        public Long inQuantity;
        public Long outQuantity;
        public String remark;
    }

    class InventoryFlowResponseDto {
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

    // Reservation
    class ReservationRequest {
        public Long branchProductId;
        public Long quantity;
        public String reason;
        public Long purchaseOrderId;
    }

    // 마지막 조회 상품
    @GetMapping("/customer/product/view/{memberId}")
    public CommonSuccessDto getProductId(@PathVariable Long memberId);



    // 속성
    class ProductAttributeValueResponseDto {
        public Long id;
        public Long productId;
        public Long attributeValueId;
        public Long attributeTypeId;
        public String attributeTypeName;
        public String displayName;
    }
}
