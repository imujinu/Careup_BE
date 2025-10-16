package com.careup.branch.common.util;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 인증 관련 유틸리티 클래스
 */
public class AuthenticationUtils {

    /**
     * Spring Security Context에서 현재 인증된 사용자의 employeeId 추출
     * @return 현재 로그인한 직원의 ID
     * @throws IllegalStateException 인증되지 않았거나 토큰에 employeeId가 없는 경우
     */
    public static Long getAuthenticatedEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Claims claims = (Claims) authentication.getDetails();
        Long employeeId = claims.get("employeeId", Long.class);

        if (employeeId == null) {
            throw new IllegalStateException("토큰에서 직원 ID를 찾을 수 없습니다.");
        }

        return employeeId;
    }

    /**
     * Spring Security Context에서 현재 인증된 사용자의 역할(role) 추출
     * @return 현재 로그인한 사용자의 역할
     */
    public static String getAuthenticatedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Claims claims = (Claims) authentication.getDetails();
        return String.valueOf(claims.get("role"));
    }
}

