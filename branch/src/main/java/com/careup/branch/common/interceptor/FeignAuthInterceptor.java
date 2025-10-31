package com.careup.branch.common.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class FeignAuthInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Authentication이 null이거나 인증되지 않은 경우
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        // 인증이 필요 없는 경우에도 토큰이 없으면 그냥 진행
        Object credentials = authentication.getCredentials();
        if (credentials instanceof String token) {
            template.header("Authorization", "Bearer " + token);
        }
    }
}
