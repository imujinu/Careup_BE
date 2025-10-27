package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.SalesForecastDto;
import com.careup.branch.domain.branch.dto.request.BulkSalesForecastRequest;
import com.careup.branch.domain.branch.dto.request.SalesForecastRequest;
import com.careup.branch.domain.branch.dto.response.SalesForecastCreateResponse;
import com.careup.branch.domain.branch.dto.response.SalesForecastResponse;
import com.careup.branch.domain.branch.service.SalesForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sales-forecast")
@RequiredArgsConstructor
public class SalesForecastController {

    private final SalesForecastService salesForecastService;

    /**
     * 지점 관리자 - 소속 가맹점의 예상 매출액 조회
     * GET /sales-forecast/branch/{branchId}
     */
    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'FRANCHISE_OWNER', 'HQ_ADMIN')")
    public ResponseEntity<?> getBranchSalesForecast(@PathVariable Long branchId) {
        try {
            SalesForecastResponse response = salesForecastService.getBranchSalesForecast(branchId);
            log.info("소속 가맹점의 예상 매출액 조회 성공: {}", response.toString());
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("소속 가맹점의 예상 매출액 조회")
                            .build()
            );
        } catch (Exception e) {
            log.error("소속 가맹점의 예상 매출액 조회 실패 - branchId: {}, error: {}", branchId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("조회 실패. error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 본사 관리자 - 특정 지점의 예상 매출액 계산
     * GET /sales-forecast/calculate/{branchId}?forecastDays=30
     */
    @GetMapping("/calculate/{branchId}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> calculateSalesForecast(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "30") Integer forecastDays) {
        try {
            SalesForecastDto forecast = salesForecastService.calculateSalesForecast(branchId, forecastDays);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(forecast)
                            .status_code(HttpStatus.OK.value())
                            .status_message("특정 지점의 예상 매출액 계산")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("계산 실패. error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 본사 관리자 - 가맹점의 예상 매출액 전송 (단일)
     * POST /sales-forecast
     */
    @PostMapping
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> saveSalesForecast(
            @RequestBody SalesForecastRequest request) {
        try {
            SalesForecastCreateResponse response = salesForecastService.saveSalesForecast(request);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("가맹점의 예상 매출액 전송 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("조회 실패. error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 본사 관리자 - 여러 가맹점의 예상 매출액 일괄 전송
     * POST /sales-forecast/bulk
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> saveBulkSalesForecasts(
            @RequestBody BulkSalesForecastRequest request) {
        try {
            List<SalesForecastCreateResponse> responses = salesForecastService.saveBulkSalesForecasts(request);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(responses)
                            .status_code(HttpStatus.OK.value())
                            .status_message("여러 가맹점 예상 매출액 일괄 전송 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("전송 실패. error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 본사 관리자 - 모든 지점의 예상 매출액 자동 계산 및 전송
     * POST /sales-forecast/calculate-all?forecastDays=30
     */
    @PostMapping("/calculate-all")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> calculateAndSaveAllBranchForecasts(
            @RequestParam(defaultValue = "30") Integer forecastDays) {
        try {
            List<SalesForecastCreateResponse> responses =
                    salesForecastService.calculateAndSaveAllBranchForecasts(forecastDays);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(responses)
                            .status_code(HttpStatus.OK.value())
                            .status_message("모든 지점 예상 매출액 자동 계산 및 전송 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("전송 실패. error: " + e.getMessage())
                            .build()
            );
        }
    }
}

