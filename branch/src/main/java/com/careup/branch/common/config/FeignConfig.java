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

@Slf4j
@Configuration
public class FeignConfig {

    /**
     * Feign 요청 시 현재 요청의 Authorization 헤더를 자동으로 전달하는 Interceptor
     */
    @Bean
    public RequestInterceptor requestTokenBearerInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 현재 HTTP 요청에서 Authorization 헤더 추출
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    String authorization = attributes.getRequest().getHeader("Authorization");
                    if (authorization != null && authorization.startsWith("Bearer ")) {
                        log.debug("Feign 요청에 Authorization 헤더 추가: {}", authorization.substring(0, 20) + "...");
                        template.header("Authorization", authorization);
                        return;
                    }
                }

                // SecurityContext에서 인증 정보 확인 (fallback)
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    log.debug("SecurityContext에서 인증 정보 확인: {}", authentication.getName());
                    // 필요시 추가 로직
                }

                log.warn("Feign 요청에 Authorization 헤더를 추가할 수 없습니다.");
            }
        };
    }
}

