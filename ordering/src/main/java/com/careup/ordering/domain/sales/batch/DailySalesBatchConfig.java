package com.careup.ordering.domain.sales.batch;

import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.statistics.entity.DailyBranchSalesStatistic;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class DailySalesBatchConfig {

    // Spring Batch 5.x부터는 Factory 대신 JobRepository와 TransactionManager를 직접 주입받아 사용해
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    // Processor와 Writer (이후 구현 예정)
    private final DailySalesItemProcessor dailySalesItemProcessor;
    private final DailySalesItemWriter dailySalesItemWriter;

    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job dailyBranchSalesJob() {
        return new JobBuilder("dailyBranchSalesJob", jobRepository)
                .start(dailyBranchSalesStep())
                .build();
    }

    @Bean
    public Step dailyBranchSalesStep() {
        return new StepBuilder("dailyBranchSalesStep", jobRepository)
                .<Order, DailyBranchSalesStatistic>chunk(CHUNK_SIZE, transactionManager) // Chunk 단위 처리
                .reader(orderReader())
                .processor(dailySalesItemProcessor)
                .writer(dailySalesItemWriter)
                .faultTolerant()
                .skip(Exception.class) // 데이터 오류 시 해당 아이템 스킵 [cite: 82, 97]
                .skipLimit(10)
                .retry(DeadlockLoserDataAccessException.class) // 데드락 발생 시 재시도 [cite: 82, 97]
                .retryLimit(3)
                .build();
    }

    @Bean
    public JpaPagingItemReader<Order> orderReader() {
        // 전일 자정부터 당일 자정 전까지의 주문 조회
        LocalDateTime start = LocalDateTime.now().minusDays(1).with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().minusDays(1).with(LocalTime.MAX);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", start);
        parameters.put("end", end);

        return new JpaPagingItemReaderBuilder<Order>()
                .name("orderReader")
                .entityManagerFactory(entityManagerFactory)
                // PENDING(대기)이나 CANCELLED(취소) 상태는 매출 통계에서 제외하기 위해 CONFIRMED 조건 추가
                .queryString("SELECT o FROM Order o WHERE o.createdAt BETWEEN :start AND :end AND o.orderStatus = 'CONFIRMED'")
                .parameterValues(parameters)
                .pageSize(CHUNK_SIZE)
                .build();
    }
}