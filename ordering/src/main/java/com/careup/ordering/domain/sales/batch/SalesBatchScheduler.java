package com.careup.ordering.domain.sales.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job dailyBranchSalesJob;

    // 매일 새벽 2시에 실행 (Cron: 초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 2 * * *")
    public void runDailySalesBatch() {
        // 1. 어제 날짜 계산 (예: 오늘이 15일이면 14일 데이터 집계)
        String yesterday = LocalDate.now().minusDays(1).toString();

        // 2. JobParameters 설정 (날짜 + 실행시간을 넣어 중복 실행 방지)
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("requestDate", yesterday)
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        try {
            log.info(">>> 일일 매출 집계 배치 시작: {}", yesterday);
            jobLauncher.run(dailyBranchSalesJob, jobParameters);
        } catch (Exception e) {
            log.error(">>> 배치 실행 중 에러 발생: {}", e.getMessage());
            // 여기서 알림 발송(슬랙 등) 로직을 넣으면 완벽합니다!
        }
    }
}
