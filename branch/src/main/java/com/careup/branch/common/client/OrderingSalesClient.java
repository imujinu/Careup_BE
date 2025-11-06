package com.careup.branch.common.client;

import com.careup.branch.common.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Ordering 서비스의 매출 통계 API를 호출하는 FeignClient
 * Eureka를 통해 ordering-service와 직접 통신
 */
@FeignClient(name = "ordering-service", configuration = FeignConfig.class)
public interface OrderingSalesClient {

    /**
     * ordering 서비스에서 특정 지점의 매출 통계 조회
     * 예상 매출 계산을 위한 30일 평균 매출 데이터 제공
     */
    @GetMapping("/sales/statistics-for-forecast")
    Map<String, Object> getSalesStatisticsForForecast(
            @RequestParam("branchId") Long branchId,
            @RequestParam("days") Integer days);
}

