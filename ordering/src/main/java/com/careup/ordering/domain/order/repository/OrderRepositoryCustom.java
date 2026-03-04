package com.careup.ordering.domain.order.repository;

import com.careup.ordering.domain.order.dto.AllBranchesSalesDto;
import com.careup.ordering.domain.order.dto.SalesStatisticsDto;
import com.careup.ordering.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepositoryCustom {
    List<SalesStatisticsDto> findMonthlySalesStatistics(OrderStatus status, LocalDateTime start, LocalDateTime end);
    AllBranchesSalesDto calculateTotalSalesStats(LocalDateTime start, LocalDateTime end);
}
