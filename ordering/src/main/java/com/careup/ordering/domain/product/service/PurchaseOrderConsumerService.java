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
    @KafkaListener(topics = "purchase-order-approved", groupId = "ordering-group", containerFactory = "purchaseOrderKafkaListenerContainerFactory")
    public void handlePurchaseOrderApproved(PurchaseOrderEventDto event) {
        
        try {
            // 발주 승인 시 본사의 재고를 차감
            for (PurchaseOrderEventDto.PurchaseOrderDetailEventDto orderDetail : event.getOrderDetails()) {
                Long hqBranchId = 1L;
                Long branchProductId = findBranchProductId(hqBranchId, orderDetail.getProductId());
                
                if (branchProductId != null) {
                    // 시스템 내부 호출이므로 Authentication 없이 처리
                    inventoryService.adjustStock(
                        branchProductId,
                        (long) orderDetail.getQuantity(),
                        "DECREASE",
                        "발주 승인으로 인한 재고 차감", null // 시스템 내부 호출
                    );

                } else {
                }
            }
            
        } catch (Exception e) {
           log.error("발주 승인 실패: purchaseOrderId={}", event.getPurchaseOrderId(), e);
       }
   }

   // 발주 입고 완료 이벤트 수신 및 가맹점 재고 증가 처리
   @KafkaListener(topics = "purchase-order-completed", groupId = "ordering-group", containerFactory = "purchaseOrderKafkaListenerContainerFactory")
   public void handlePurchaseOrderCompleted(PurchaseOrderEventDto event) {
       try {
           // 가맹점 재고 증가
           for (PurchaseOrderEventDto.PurchaseOrderDetailEventDto orderDetail : event.getOrderDetails()) {
               Long branchProductId = findBranchProductId(event.getBranchId(), orderDetail.getProductId());

               if (branchProductId != null) {
                   inventoryService.adjustStock(
                       branchProductId,
                       (long) orderDetail.getQuantity(),
                       "INCREASE",
                       "발주 입고 완료로 인한 재고 증가", null);

               } else {
                   log.warn("BranchProduct를 찾을 수 없음: branchId={}, productId={}",
                       event.getBranchId(), orderDetail.getProductId());
               }
           }


       } catch (Exception e) {
           log.error("발주 입고 완료 실패: purchaseOrderId={}", event.getPurchaseOrderId(), e);
       }
   }

   /**
    * branchId와 productId로 BranchProduct를 조회하여 branchProductId를 반환
    * - 발주 승인 시: 본사의 재고를 차감
    * - 입고 완료 시: 가맹점의 재고를 증가
    */
   private Long findBranchProductId(Long branchId, Long productId) {
       return branchProductRepository.findByBranchIdAndProductId(branchId, productId)
           .map(branchProduct -> branchProduct.getId())
           .orElse(null);
   }
}
