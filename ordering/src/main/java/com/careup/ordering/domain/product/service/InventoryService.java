package com.careup.ordering.domain.product.service;

import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.InventoryFlowDetail;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import com.careup.ordering.domain.product.repository.InventoryFlowDetailRepository;
import com.careup.ordering.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final BranchProductRepository branchProductRepository;
    private final InventoryFlowDetailRepository inventoryFlowDetailRepository;
    private final ProductRepository productRepository;

    // 지점별 상품 등록
    public BranchProduct createBranchProduct(Long productId, Long branchId, String serialNumber,
                                           Long stockQuantity, Long safetyStock, Long price) {
        // Product 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
        
        // BranchProduct 생성
        BranchProduct branchProduct = BranchProduct.builder()
                .product(product)
                .branchId(branchId)
                .serialNumber(serialNumber)
                .stockQuantity(stockQuantity != null ? stockQuantity : 0L)
                .safetystock(safetyStock != null ? safetyStock : 0L)
                .price(price)
                .build();
        
        return branchProductRepository.save(branchProduct);
    }

    // 지점별 재고 조회
    @Transactional(readOnly = true)
    public List<BranchProduct> getBranchProducts(Long branchId) {
        return branchProductRepository.findByBranchId(branchId);
    }

    // 특정 상품의 지점별 재고 조회
    @Transactional(readOnly = true)
    public BranchProduct getBranchProduct(Long branchId, Long productId) {
        return branchProductRepository.findByBranchIdAndProductId(branchId, productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지점의 상품을 찾을 수 없습니다"));
    }

    // 안전재고 설정
    public void updateSafetyStock(Long branchProductId, Long safetyStock) {
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));
        
        branchProduct.updateSafetyStock(safetyStock);
        branchProductRepository.save(branchProduct);
    }

    // 재고 증감
    public void adjustStock(Long branchProductId, Long quantity, String type, String reason) {
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));
        
        if ("INCREASE".equals(type)) {
            branchProduct.increaseStock(quantity);
        } else if ("DECREASE".equals(type)) {
            branchProduct.decreaseStock(quantity);
        }
        
        branchProductRepository.save(branchProduct);
        
        // 입출고 기록 생성
        InventoryFlowDetail flow = InventoryFlowDetail.builder()
                .branchProduct(branchProduct)
                .inQuantity("INCREASE".equals(type) ? quantity : null)
                .outQuantity("DECREASE".equals(type) ? quantity : null)
                .remark(reason)
                .build();
        
        inventoryFlowDetailRepository.save(flow);
    }

    // 입출고 기록 등록
    public InventoryFlowDetail createInventoryFlow(Long branchProductId, Long inQuantity, 
                                                  Long outQuantity, String remark) {
        BranchProduct branchProduct = branchProductRepository.findById(branchProductId)
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + branchProductId));
        
        InventoryFlowDetail flow = InventoryFlowDetail.builder()
                .branchProduct(branchProduct)
                .inQuantity(inQuantity)
                .outQuantity(outQuantity)
                .remark(remark)
                .build();
        
        InventoryFlowDetail savedFlow = inventoryFlowDetailRepository.save(flow);
        
        // 재고 업데이트
        if (inQuantity != null) {
            branchProduct.increaseStock(inQuantity);
        }
        if (outQuantity != null) {
            branchProduct.decreaseStock(outQuantity);
        }
        branchProductRepository.save(branchProduct);
        
        return savedFlow;
    }

    // 입출고 기록 조회
    @Transactional(readOnly = true)
    public List<InventoryFlowDetail> getInventoryFlows(Long branchId, Long productId) {
        if (branchId != null && productId != null) {
            return inventoryFlowDetailRepository.findByBranchIdAndProductId(branchId, productId);
        } else if (branchId != null) {
            return inventoryFlowDetailRepository.findByBranchId(branchId);
        } else {
            return inventoryFlowDetailRepository.findAll();
        }
    }

    // 입출고 기록 삭제
    public void deleteInventoryFlow(Long flowId) {
        InventoryFlowDetail flow = inventoryFlowDetailRepository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("입출고 기록을 찾을 수 없습니다: " + flowId));
        
        inventoryFlowDetailRepository.delete(flow);
    }

    // 재고 조절 내역 조회 (불량/폐기)
    @Transactional(readOnly = true)
    public List<InventoryFlowDetail> getAdjustmentHistory(Long branchId, String reason) {
        if (branchId != null && reason != null) {
            return inventoryFlowDetailRepository.findByBranchIdAndRemarkContaining(branchId, reason);
        } else if (branchId != null) {
            return inventoryFlowDetailRepository.findByBranchId(branchId);
        } else if (reason != null) {
            return inventoryFlowDetailRepository.findByRemarkContaining(reason);
        } else {
            return inventoryFlowDetailRepository.findAll();
        }
    }
}
