package com.careup.branch.common.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new FeignRequestInterceptor();
    }

    public static class FeignRequestInterceptor implements RequestInterceptor {

        @Override
        public void apply(RequestTemplate requestTemplate) {
            // 1. 현재 HTTP 요청에서 Authorization 헤더 가져오기
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authorizationHeader = request.getHeader("Authorization");

                if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                    requestTemplate.header("Authorization", authorizationHeader);
                    log.debug("Added Authorization header to Feign request: {}",
                        authorizationHeader.substring(0, Math.min(20, authorizationHeader.length())) + "...");
                    return;
                }
            }

            // 2. SecurityContext에서 Authentication 정보 가져오기
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getCredentials() != null) {
                String token = authentication.getCredentials().toString();
                if (token != null && !token.isEmpty()) {
                    // Bearer 접두사가 없으면 추가
                    String bearerToken = token.startsWith("Bearer ") ? token : "Bearer " + token;
                    requestTemplate.header("Authorization", bearerToken);
                    log.debug("Added Authorization header from SecurityContext to Feign request");
                }
            } else {
                log.warn("No authentication information available for Feign request to: {}",
                    requestTemplate.url());
            }
        }
    }
}
