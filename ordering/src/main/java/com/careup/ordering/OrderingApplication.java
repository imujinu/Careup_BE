package com.careup.ordering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class OrderingApplication {
	//테스트
	public static void main(String[] args) {
		SpringApplication.run(OrderingApplication.class, args);
	}

}
