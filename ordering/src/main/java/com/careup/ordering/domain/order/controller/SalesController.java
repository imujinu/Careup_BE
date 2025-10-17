package com.careup.ordering.domain.order.controller;

import com.careup.ordering.common.dto.CommonErrorDto;
import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.order.dto.BranchComparisonDto;
import com.careup.ordering.domain.order.dto.request.SalesStatisticsRequestDto;
import com.careup.ordering.domain.order.dto.response.ProductSalesResponseDto;
import com.careup.ordering.domain.order.dto.response.SalesStatisticsResponseDto;
import com.careup.ordering.domain.order.dto.response.SalesForecastResponseDto;
import com.careup.ordering.domain.order.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    /**
     * 매출 통계 조회 (요일별, 시간별, 기간별)
     * GET /sales/statistics?branchId=1&startDate=2025-01-01&endDate=2025-01-31&periodType=DAY
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'FRANCHISE_OWNER')")
    public ResponseEntity<?> getSalesStatistics(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType) {

        try {
            SalesStatisticsRequestDto request = new SalesStatisticsRequestDto(branchId, startDate, endDate, periodType);
            SalesStatisticsResponseDto response = salesService.getSalesStatistics(request);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("매출 통계 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("매출 통계 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 상품별 매출 조회 (마진율 높은 상품, 판매량 많은 상품 등)
     * GET /sales/products?branchId=1&startDate=2025-01-01&endDate=2025-01-31&sortType=HIGH_MARGIN
     */
    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'FRANCHISE_OWNER')")
    public ResponseEntity<?> getProductSales(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "HIGH_SALES") String sortType) {

        try {
            ProductSalesResponseDto response = salesService.getProductSales(branchId, startDate, endDate, sortType);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("상품별 매출 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("상품별 매출 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 인근 지역 가맹점 평균 및 매출 비교 (위치 기반 - MSA 개선)
     * GET /sales/comparison?branchId=1&startDate=2025-01-01&endDate=2025-01-31&radiusKm=10
     */
    @GetMapping("/comparison")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'FRANCHISE_OWNER')")
    public ResponseEntity<?> compareBranchSales(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {

        try {
            List<BranchComparisonDto> response = salesService.compareBranchSales(
                    branchId, startDate, endDate, radiusKm);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("인근 지역 가맹점 평균 및 매출 비교 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("인근 지역 가맹점 평균 및 매출 비교 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 소속 가맹점의 예상 매출액 조회 (branch 모듈의 SalesForecast 사용)
     * GET /sales/forecast?branchId=1&targetDate=2025-01-31
     */
    @GetMapping("/forecast")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'FRANCHISE_OWNER')")
    public ResponseEntity<?> getSalesForecast(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate) {

        try {
            SalesForecastResponseDto response = salesService.getSalesForecast(branchId, targetDate);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(response)
                            .status_code(HttpStatus.OK.value())
                            .status_message("소속 가맹점의 예상 매출액 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("소속 가맹점의 예상 매출액 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * branch 서비스용 - 예상 매출 계산을 위한 매출 통계 조회
     * GET /sales/statistics-for-forecast?branchId=1&days=30
     */
    @GetMapping("/statistics-for-forecast")
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'FRANCHISE_OWNER')")
    public ResponseEntity<?> getSalesStatisticsForForecast(
            @RequestParam Long branchId,
            @RequestParam(defaultValue = "30") Integer days) {

        try {
            Map<String, Object> statistics = salesService.getSalesStatisticsForForecast(branchId, days);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(statistics)
                            .status_code(HttpStatus.OK.value())
                            .status_message("예상 매출 계산을 위한 매출 통계 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("조회 실폐. error: " + e.getMessage())
                            .build()
            );
        }
    }
}
