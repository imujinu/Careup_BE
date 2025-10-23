package com.careup.ordering.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

/**
 * 토스 페이먼츠 설정
 */
@Configuration
@Getter
public class TossPaymentConfig {
    
    @Value("${payment.toss.client-key}")
    private String clientKey;
    
    @Value("${payment.toss.secret-key}")
    private String secretKey;
    
    @Value("${payment.toss.security-key}")
    private String securityKey;
    
    @Value("${payment.toss.api-url:https://api.tosspayments.com}")
    private String apiUrl;
    
    /**
     * 시크릿 키를 Base64로 인코딩
     * 토스는 시크릿 키를 Base64 인코딩해서 Authorization 헤더에 넣어야 함
     */
    public String getEncodedSecretKey() {
        return Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
    }
}
