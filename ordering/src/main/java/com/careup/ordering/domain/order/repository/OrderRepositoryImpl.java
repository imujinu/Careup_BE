package com.careup.ordering.domain.order.repository;

import com.careup.ordering.domain.order.dto.AllBranchesSalesDto;
import com.careup.ordering.domain.order.dto.QAllBranchesSalesDto;
import com.careup.ordering.domain.order.dto.SalesStatisticsDto;
import com.careup.ordering.domain.order.entity.OrderStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import static com.careup.ordering.domain.order.entity.QOrder.order;
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    @Override
    public List<SalesStatisticsDto> findMonthlySalesStatistics(OrderStatus status, LocalDateTime start, LocalDateTime end) {
        return queryFactory
                .select(Projections.constructor(SalesStatisticsDto.class,
                        order.createdAt.min(),
                        order.totalAmount.sum(),
                        order.count()
                ))
                .from(order)
                .where(order.orderStatus.eq(status), order.createdAt.between(start, end))
                .groupBy(order.createdAt.year(), order.createdAt.month())
                .fetch();
    }

    public AllBranchesSalesDto calculateTotalSalesStats(LocalDateTime start, LocalDateTime end) {
        return queryFactory
                .select(new QAllBranchesSalesDto(
                        Expressions.asString(start.toLocalDate().toString()),
                        Expressions.asString("TOTAL"),
                        order.totalAmount.sum().coalesce(0L),
                        order.count(),
                        order.totalAmount.sum().divide(order.count().nullif(0L)).longValue().coalesce(0L),
                        order.branchId.countDistinct(),
                        order.totalAmount.sum().divide(order.branchId.countDistinct().nullif(0L)).longValue().coalesce(0L)
                ))
                .from(order)
                .where(
                        order.orderStatus.eq(OrderStatus.CONFIRMED),
                        order.createdAt.between(start, end)
                )
                .fetchOne();
    }

}
