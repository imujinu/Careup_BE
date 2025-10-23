package com.careup.branch.common.client;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.chat.dto.req.ChatOrderDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "ordering-service-chat", url = "${feign.ordering.url:http://localhost:8080}")
public interface ChatFeignClient {
    // [일일 매출 조회]
    @GetMapping("/{branchId}/sales/today")
    CommonSuccessDto getDailyPayments(@PathVariable Long branchId);

    // [매출 예측]
    @GetMapping("/{branchId}/sales/week")
    CommonSuccessDto getPredictDailySales(Long branchId);

    // [매출 예측]
    @GetMapping("/{branchId}/sales/labor")
    CommonSuccessDto getLaborCost(Long branchId);
    // [재고 조회]
    @GetMapping("/{branchId}/stock/day")
    CommonSuccessDto getDailyStock(@PathVariable Long branchId);

    // [발주 요청]
    @GetMapping("/{branchId}/order")
    CommonSuccessDto createOrder(@PathVariable ChatOrderDto dto);

    // [고객 조회]
    @GetMapping("/{branchId}/visits")
    CommonSuccessDto getTodayVisits(Long branchId);

    @GetMapping("/branchProducts/all")
    CommonSuccessDto getAllStock(@RequestParam Long branchId);
    @GetMapping("/branchProducts/safety")
    CommonSuccessDto getSafetyStock(@RequestParam Long id);



    // [ 매출 ]

    // [ 매출 ] - 본사 직원용

    // 전체 매출 조회
    @GetMapping("/hq/sales/all")
    CommonSuccessDto getAllBranchesSales(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType);

    // 특정 지점 매출 내역 조회
    @GetMapping("/hq/sales/branch/{branchId}")
    CommonSuccessDto getBranchSalesDetail(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType);

    //가맹점 간 매출 비교
    @GetMapping("/hq/sales/comparison")
    CommonSuccessDto compareBranchesSales(
            @RequestParam List<Long> branchIds,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType);

    // [ 매출 ] - 지점용

    // 기간별 매출 조회
    @GetMapping("/sales/statistics")
    CommonSuccessDto getSalesStatistics(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType);


    // 상품별 매출 조회
    @GetMapping("/sales/products")
    CommonSuccessDto getProductSales(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String sortType);

    // 인근 지점 매출 비교
    @GetMapping("/sales/comparison")
    CommonSuccessDto compareBranchSales(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "10.0") Double radiusKm);

    // 지점 매출 예측
    @GetMapping("/sales/forecast")
    CommonSuccessDto getSalesForecast(
            @RequestParam Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate);


    @GetMapping("/inventory/branch/${branchId}")
    CommonSuccessDto getBranchProducts(@PathVariable Long branchId);


}