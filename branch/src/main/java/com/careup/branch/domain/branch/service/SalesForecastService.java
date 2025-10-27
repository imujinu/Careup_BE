package com.careup.branch.domain.branch.service;

import com.careup.branch.common.client.OrderingSalesClient;
import com.careup.branch.domain.branch.dto.SalesForecastDto;
import com.careup.branch.domain.branch.dto.request.BulkSalesForecastRequest;
import com.careup.branch.domain.branch.dto.request.SalesForecastRequest;
import com.careup.branch.domain.branch.dto.response.SalesForecastCreateResponse;
import com.careup.branch.domain.branch.dto.response.SalesForecastResponse;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.SalesForecast;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.branch.repository.SalesForecastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesForecastService {

    private final SalesForecastRepository salesForecastRepository;
    private final BranchRepository branchRepository;
    private final OrderingSalesClient orderingSalesClient;

    /**
     * 지점 관리자 - 소속 가맹점의 예상 매출액 조회
     */
    public SalesForecastResponse getBranchSalesForecast(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점입니다."));

        // 현재 적용 중인 예상 매출 조회
        Date today = new Date();
        Optional<SalesForecast> currentForecast = salesForecastRepository
                .findByBranchIdAndPeriod(branchId, today);

        // 예상 매출 이력 조회
        List<SalesForecast> forecastHistory = salesForecastRepository
                .findByBranchIdOrderByPeriodStartDesc(branchId);

        return SalesForecastResponse.builder()
                .branchId(branchId)
                .branchName(branch.getName())
                .currentForecast(currentForecast.map(this::convertToDto).orElse(null))
                .forecastHistory(forecastHistory.stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 본사 관리자 - 예상 매출액 계산 (ordering 서비스의 매출 데이터 기반)
     */
    public SalesForecastDto calculateSalesForecast(Long branchId, Integer forecastDays) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점입니다."));

        try {
            // ordering 서비스에서 지난 30일 매출 통계 조회
            Map<String, Object> salesStats = orderingSalesClient
                    .getSalesStatisticsForForecast(branchId, 30);

            // 매출 통계 기반 예상 매출 계산
            Long averageDailySales = ((Number) salesStats.getOrDefault("averageDailySales", 0L)).longValue();

            // 요일별 가중치 적용
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(forecastDays != null ? forecastDays : 30);

            Long forecastAmount = calculateForecastWithDayWeight(averageDailySales, startDate, endDate);

            return SalesForecastDto.builder()
                    .branchId(branchId)
                    .branchName(branch.getName())
                    .amount(forecastAmount)
                    .periodStart(Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .periodEnd(Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .forecastBasis("지난 30일 평균 매출 기반 + 요일별 가중치")
                    .build();

        } catch (Exception e) {
            log.error("매출 통계 조회 실패: {}", e.getMessage());
            // Feign 통신 실패 시 기본값 반환
            return getDefaultForecast(branch, forecastDays);
        }
    }

    /**
     * 본사 관리자 - 가맹점의 예상 매출액 전송 (단일)
     * 같은 기간에 대한 예상 매출이 이미 있으면 업데이트
     */
    @Transactional
    public SalesForecastCreateResponse saveSalesForecast(SalesForecastRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점입니다."));

        // 같은 기간의 기존 예상 매출 조회
        List<SalesForecast> existingForecasts = salesForecastRepository
                .findByBranchIdAndPeriodList(request.getBranchId(), request.getPeriodStart());

        SalesForecast salesForecast;
        String message;

        if (!existingForecasts.isEmpty()) {
            // 기존 데이터가 있으면 가장 최신 것을 업데이트
            salesForecast = existingForecasts.get(0);
            salesForecast.updateAmount(request.getAmount());
            salesForecast.updatePeriod(request.getPeriodStart(), request.getPeriodEnd());
            message = "예상 매출액이 성공적으로 업데이트되었습니다.";
            log.info("기존 예상 매출 업데이트 - branchId: {}, forecastId: {}", branch.getId(), salesForecast.getId());
        } else {
            // 신규 생성
            salesForecast = SalesForecast.builder()
                    .branch(branch)
                    .amount(request.getAmount())
                    .periodStart(request.getPeriodStart())
                    .period_end(request.getPeriodEnd())
                    .build();
            salesForecast = salesForecastRepository.save(salesForecast);
            message = "예상 매출액이 성공적으로 등록되었습니다.";
            log.info("새로운 예상 매출 생성 - branchId: {}", branch.getId());
        }

        return SalesForecastCreateResponse.builder()
                .id(salesForecast.getId())
                .branchId(branch.getId())
                .branchName(branch.getName())
                .amount(salesForecast.getAmount())
                .message(message)
                .build();
    }

    /**
     * 본사 관리자 - 여러 가맹점의 예상 매출액 일괄 전송
     */
    @Transactional
    public List<SalesForecastCreateResponse> saveBulkSalesForecasts(BulkSalesForecastRequest request) {
        List<SalesForecastCreateResponse> responses = new ArrayList<>();

        for (SalesForecastRequest forecastRequest : request.getForecasts()) {
            try {
                SalesForecastCreateResponse response = saveSalesForecast(forecastRequest);
                responses.add(response);
            } catch (Exception e) {
                log.error("지점 {} 예상 매출 저장 실패: {}", forecastRequest.getBranchId(), e.getMessage());
                responses.add(SalesForecastCreateResponse.builder()
                        .branchId(forecastRequest.getBranchId())
                        .message("예상 매출액 등록 실패: " + e.getMessage())
                        .build());
            }
        }

        return responses;
    }

    /**
     * 본사 관리자 - 모든 지점의 예상 매출액 자동 계산 및 전송
     */
    @Transactional
    public List<SalesForecastCreateResponse> calculateAndSaveAllBranchForecasts(Integer forecastDays) {
        List<Branch> branches = branchRepository.findAll();
        List<SalesForecastCreateResponse> responses = new ArrayList<>();

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(forecastDays != null ? forecastDays : 30);
        Date periodStart = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date periodEnd = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        for (Branch branch : branches) {
            try {
                // 각 지점의 예상 매출 계산
                SalesForecastDto forecastDto = calculateSalesForecast(branch.getId(), forecastDays);

                // 예상 매출 저장
                SalesForecastRequest request = new SalesForecastRequest(
                        branch.getId(),
                        forecastDto.getAmount(),
                        periodStart,
                        periodEnd
                );

                SalesForecastCreateResponse response = saveSalesForecast(request);
                responses.add(response);

            } catch (Exception e) {
                log.error("지점 {} 예상 매출 자동 생성 실패: {}", branch.getId(), e.getMessage());
                responses.add(SalesForecastCreateResponse.builder()
                        .branchId(branch.getId())
                        .branchName(branch.getName())
                        .message("예상 매출액 자동 생성 실패: " + e.getMessage())
                        .build());
            }
        }

        return responses;
    }

    /**
     * 요일별 가중치를 적용한 예상 매출 계산
     */
    private Long calculateForecastWithDayWeight(Long averageDailySales, LocalDate startDate, LocalDate endDate) {
        long totalDays = endDate.toEpochDay() - startDate.toEpochDay();
        double totalWeight = 0.0;

        for (long i = 0; i < totalDays; i++) {
            LocalDate date = startDate.plusDays(i);
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            totalWeight += getDayWeightFactor(dayOfWeek);
        }

        return (long) (averageDailySales * totalWeight);
    }

    /**
     * 요일별 가중치 반환
     */
    private double getDayWeightFactor(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case SATURDAY:
            case SUNDAY:
                return 1.3; // 주말 30% 증가
            case FRIDAY:
                return 1.15; // 금요일 15% 증가
            case MONDAY:
                return 0.9; // 월요일 10% 감소
            default:
                return 1.0; // 평일 기본
        }
    }

    /**
     * Feign 통신 실패 시 기본 예상 매출 반환
     */
    private SalesForecastDto getDefaultForecast(Branch branch, Integer forecastDays) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(forecastDays != null ? forecastDays : 30);

        // 최근 예상 매출이 있다면 그 값을 기준으로 설정
        Optional<SalesForecast> latestForecast = salesForecastRepository
                .findFirstByBranchIdOrderByCreatedAtDesc(branch.getId());

        Long defaultAmount = latestForecast.map(SalesForecast::getAmount).orElse(1000000L);

        return SalesForecastDto.builder()
                .branchId(branch.getId())
                .branchName(branch.getName())
                .amount(defaultAmount)
                .periodStart(Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .periodEnd(Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .forecastBasis("매출 통계 조회 실패 - 기본값 또는 이전 예상 매출 기반")
                .build();
    }

    /**
     * Entity to DTO 변환
     */
    private SalesForecastDto convertToDto(SalesForecast salesForecast) {
        // LocalDateTime to Date 변환
        Date createdAt = salesForecast.getCreatedAt() != null ?
                Date.from(salesForecast.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()) : null;

        return SalesForecastDto.builder()
                .id(salesForecast.getId())
                .branchId(salesForecast.getBranch().getId())
                .branchName(salesForecast.getBranch().getName())
                .amount(salesForecast.getAmount())
                .periodStart(salesForecast.getPeriodStart())
                .periodEnd(salesForecast.getPeriod_end())
                .createdAt(createdAt)
                .forecastBasis("저장된 예상 매출")
                .build();
    }
}

