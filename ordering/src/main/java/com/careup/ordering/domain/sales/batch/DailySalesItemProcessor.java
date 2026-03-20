package com.careup.ordering.domain.sales.batch;

import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.statistics.entity.DailyBranchSalesStatistic;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class DailySalesItemProcessor implements ItemProcessor<Order, DailyBranchSalesStatistic> {

    @Override
    public DailyBranchSalesStatistic process(Order order) throws Exception {
        // 단건 주문 데이터를 통계 엔티티의 초기 형태로 변환 (주문 건수 = 1)
        return DailyBranchSalesStatistic.builder()
                .branchId(order.getBranchId())
                .salesDate(order.getCreatedAt().toLocalDate())
                .totalSalesAmount(order.getTotalAmount())
                .totalOrderCount(1L)
                .build();
    }
}
