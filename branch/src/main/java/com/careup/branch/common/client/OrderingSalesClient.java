package com.careup.branch.common.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "ordering-service", url = "${feign.ordering.url:http://localhost:8080}")
public interface OrderingSalesClient {

    /**
     * ordering 서비스에서 특정 지점의 매출 통계 조회
     */
    @GetMapping("/sales/statistics-for-forecast")
    Map<String, Object> getSalesStatisticsForForecast(
            @RequestParam("branchId") Long branchId,
            @RequestParam("days") Integer days);
}

