package com.careup.branch.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimeConfig {

    public static final String DEFAULT_ZONE = "Asia/Seoul";

    /** 애플리케이션 전역 기본 타임존 고정 */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_ZONE));
    }

    /** 서비스/도메인 레이어에서 주입 받아 쓰는 표준 Clock */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(DEFAULT_ZONE));
    }
}
