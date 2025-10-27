package com.careup.ordering.domain.order.service;

import com.careup.ordering.common.client.BranchClient;
import com.careup.ordering.domain.order.dto.SalesForecastDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Branch 서비스와 통신하여 예상 매출액 정보를 조회하는 통합 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesForecastIntegrationService {

    private final BranchClient branchClient;

    /**
     * 모든 지점의 예상 매출액 조회
     * Branch 서비스의 API를 호출하여 데이터를 가져옵니다.
     */
    public List<SalesForecastDto> getAllBranchesSalesForecasts(Integer forecastDays) {
        try {
            log.info("모든 지점의 예상 매출액 조회 시작 - forecastDays: {}", forecastDays);

            // Branch 서비스의 FeignClient를 통해 예상 매출액 정보 조회
            // 실제 구현에서는 BranchClient에 적절한 메서드가 있어야 합니다.
            // 여기서는 예시로 더미 데이터를 반환합니다.

            // TODO: BranchClient에 getAllSalesForecasts 메서드 추가 필요
            // List<SalesForecastDto> forecasts = branchClient.getAllSalesForecasts(forecastDays);

            // 임시 더미 데이터 (실제로는 BranchClient를 통해 조회)
            List<SalesForecastDto> forecasts = new ArrayList<>();

            log.info("모든 지점의 예상 매출액 조회 완료 - 총 {}개 지점", forecasts.size());
            return forecasts;

        } catch (Exception e) {
            log.error("모든 지점의 예상 매출액 조회 실패", e);
            throw new RuntimeException("예상 매출액 조회 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 특정 지점의 예상 매출액 조회
     */
    public SalesForecastDto getBranchSalesForecast(Long branchId, Integer forecastDays) {
        try {
            log.info("지점 예상 매출액 조회 시작 - branchId: {}, forecastDays: {}", branchId, forecastDays);

            // TODO: BranchClient에 getSalesForecast 메서드 추가 필요
            // SalesForecastDto forecast = branchClient.getSalesForecast(branchId, forecastDays);

            // 임시 더미 데이터
            SalesForecastDto forecast = SalesForecastDto.builder()
                    .branchId(branchId)
                    .branchName("지점명")
                    .pastSales(10000000L)
                    .forecastedSales(12000000L)
                    .forecastDays(forecastDays)
                    .build();

            log.info("지점 예상 매출액 조회 완료 - branchId: {}", branchId);
            return forecast;

        } catch (Exception e) {
            log.error("지점 예상 매출액 조회 실패 - branchId: {}", branchId, e);
            throw new RuntimeException("예상 매출액 조회 중 오류 발생: " + e.getMessage());
        }
    }
}

