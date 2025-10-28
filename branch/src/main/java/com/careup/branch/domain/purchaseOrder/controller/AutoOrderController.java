package com.careup.branch.domain.purchaseOrder.controller;

import com.careup.branch.domain.purchaseOrder.dto.FranchiseAutoOrderSettingsDto;
import com.careup.branch.domain.purchaseOrder.dto.AutoOrderHistoryDto;
import com.careup.branch.domain.purchaseOrder.service.AutoOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auto-order")
@RequiredArgsConstructor
public class AutoOrderController {

    private final AutoOrderService autoOrderService;


    /**
     * 특정 지점의 자동 발주 실행
     */
    @PostMapping("/branch/{branchId}")
    public ResponseEntity<Map<String, Object>> executeBranchAutoOrder(@PathVariable Long branchId) {
        try {
            log.info("지점 {} 자동 발주 실행 요청", branchId);
            
            autoOrderService.executeManualAutoOrderForBranch(branchId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "지점 " + branchId + "의 자동 발주가 실행되었습니다.");
            response.put("branchId", branchId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("지점 {} 자동 발주 실행 중 오류 발생: {}", branchId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "자동 발주 실행 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 특정 상품의 자동 발주 실행
     */
    @PostMapping("/branch/{branchId}/product/{productId}")
    public ResponseEntity<Map<String, Object>> executeProductAutoOrder(
            @PathVariable Long branchId, 
            @PathVariable Long productId) {
        try {
            log.info("지점 {} 상품 {} 자동 발주 실행 요청", branchId, productId);
            
            autoOrderService.executeManualAutoOrderForProduct(branchId, productId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "상품 " + productId + "의 자동 발주가 실행되었습니다.");
            response.put("branchId", branchId);
            response.put("productId", productId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("지점 {} 상품 {} 자동 발주 실행 중 오류 발생: {}", branchId, productId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "자동 발주 실행 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 가맹점용 자동 발주 설정 조회
     */
    @GetMapping("/franchise/settings")
    public ResponseEntity<Map<String, Object>> getFranchiseAutoOrderSettings() {
        try {
            // TODO: 실제 로그인한 사용자의 branchId 사용
            Long branchId = 2L;
            
            FranchiseAutoOrderSettingsDto settings = autoOrderService.getFranchiseAutoOrderSettings(branchId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("autoOrderEnabled", settings.getAutoOrderEnabled());
            response.put("products", settings.getProducts());
            response.put("branchId", settings.getBranchId());
            response.put("updatedAt", settings.getUpdatedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("가맹점 자동 발주 설정 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 가맹점용 자동 발주 설정 업데이트
     */
    @PutMapping("/franchise/settings")
    public ResponseEntity<Map<String, Object>> updateFranchiseAutoOrderSettings(@RequestBody Map<String, Object> settings) {
        try {
            log.info("가맹점 자동 발주 설정 업데이트: {}", settings);
            
            // TODO: 실제 로그인한 사용자의 branchId 사용
            Long branchId = 2L;
            
            FranchiseAutoOrderSettingsDto updatedSettings = autoOrderService.updateFranchiseAutoOrderSettings(branchId, settings);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "자동 발주 설정이 업데이트되었습니다.");
            response.put("autoOrderEnabled", updatedSettings.getAutoOrderEnabled());
            response.put("products", updatedSettings.getProducts());
            response.put("branchId", updatedSettings.getBranchId());
            response.put("updatedAt", updatedSettings.getUpdatedAt());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("가맹점 자동 발주 설정 업데이트 중 오류 발생: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "설정 업데이트 중 오류가 발생했습니다: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 가맹점용 자동 발주 히스토리 조회
     */
    @GetMapping("/franchise/history")
    public ResponseEntity<Map<String, Object>> getFranchiseAutoOrderHistory() {
        try {
            // TODO: 실제 로그인한 사용자의 branchId 사용
            Long branchId = 2L;
            
            List<AutoOrderHistoryDto> history = autoOrderService.getFranchiseAutoOrderHistory(branchId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("autoOrders", history);
            response.put("totalCount", history.size());
            response.put("branchId", branchId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("가맹점 자동 발주 히스토리 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
