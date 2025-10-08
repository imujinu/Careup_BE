package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.dto.PurchaseOrderEventDto;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderConsumerService {

    private final InventoryService inventoryService;
    private final BranchProductRepository branchProductRepository;

    // 발주 승인 이벤트 수신 및 재고 차감
    @KafkaListener(topics = "purchase-order-approved", groupId = "ordering-group")
    public void handlePurchaseOrderApproved(PurchaseOrderEventDto event) {
        
        try {
            for (PurchaseOrderEventDto.PurchaseOrderDetailEventDto orderDetail : event.getOrderDetails()) {
                Long branchProductId = findBranchProductId(event.getBranchId(), orderDetail.getProductId());
                
                if (branchProductId != null) {
                    inventoryService.adjustStock(
                        branchProductId,
                        (long) orderDetail.getQuantity(),
                        "DECREASE",
                        "발주 승인으로 인한 재고 차감 (발주ID: " + event.getPurchaseOrderId() + ")"
                    );

                } else {
                }
            }
            
        } catch (Exception e) {
            log.error("발주 승인 이벤트 처리 실패: purchaseOrderId={}", event.getPurchaseOrderId(), e);
        }
    }

    private Long findBranchProductId(Long orderBranchId, Long productId) {
        // 발주는 본사의 재고를 차감
        Long hqBranchId = 1L;
        
        return branchProductRepository.findByBranchIdAndProductId(hqBranchId, productId)
            .map(branchProduct -> branchProduct.getId())
            .orElse(null);
    }
}
