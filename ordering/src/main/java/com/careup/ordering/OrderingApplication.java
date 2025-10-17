package com.careup.ordering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 스케줄링 기능 활성화-> 프로모션 만기되면 자동으로 만기된 프로모션이라고 하기 위해서
public class OrderingApplication {
	//테스트
	public static void main(String[] args) {
		SpringApplication.run(OrderingApplication.class, args);
	}

}
