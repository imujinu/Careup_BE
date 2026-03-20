package com.careup.ordering.domain.sales.batch;

import com.careup.ordering.domain.statistics.entity.DailyBranchSalesStatistic;
import com.careup.ordering.domain.statistics.repository.DailyBranchSalesStatisticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DailySalesItemWriter implements ItemWriter<DailyBranchSalesStatistic> {

    private final DailyBranchSalesStatisticRepository repository;

    @Override
    public void write(Chunk<? extends DailyBranchSalesStatistic> chunk) throws Exception {
        // 1. Chunk 내 데이터 메모리 집계 (지점ID_날짜 를 키로 사용)
        Map<String, DailyBranchSalesStatistic> aggregatedMap = new HashMap<>();

        for (DailyBranchSalesStatistic item : chunk) {
            String key = item.getBranchId() + "_" + item.getSalesDate();

            if (aggregatedMap.containsKey(key)) {
                // 이미 해당 지점/날짜가 맵에 있다면 금액과 건수 누적
                aggregatedMap.get(key).addStatistics(item.getTotalSalesAmount(), item.getTotalOrderCount());
            } else {
                // 없다면 새로 복사해서 맵에 추가
                aggregatedMap.put(key, DailyBranchSalesStatistic.builder()
                        .branchId(item.getBranchId())
                        .salesDate(item.getSalesDate())
                        .totalSalesAmount(item.getTotalSalesAmount())
                        .totalOrderCount(item.getTotalOrderCount())
                        .build());
            }
        }

        // 2. DB 조회 및 최종 병합 (배치 실패 시 자동 재시작(Retry) 정합성 보장 로직)
        List<DailyBranchSalesStatistic> finalToSave = new ArrayList<>();

        for (DailyBranchSalesStatistic aggregatedItem : aggregatedMap.values()) {
            repository.findByBranchIdAndSalesDate(aggregatedItem.getBranchId(), aggregatedItem.getSalesDate())
                    .ifPresentOrElse(
                            existing -> {
                                // DB에 이미 통계가 있으면 기존 데이터에 누적
                                existing.addStatistics(aggregatedItem.getTotalSalesAmount(), aggregatedItem.getTotalOrderCount());
                                finalToSave.add(existing);
                            },
                            () -> {
                                // DB에 없으면 신규 데이터로 추가
                                finalToSave.add(aggregatedItem);
                            }
                    );
        }

        // 3. JPA saveAll을 통한 Bulk 처리
        repository.saveAll(finalToSave);
    }
}