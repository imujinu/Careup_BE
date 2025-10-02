package com.careup.branch.common.auth;

import com.careup.branch.common.dto.CommonErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /// 인증 실패가 발생했을 때 호출되는 메서드
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        /// 응답 헤더/코드 세팅
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        /// 바디 생성(CommonErrorDto 포맷)
        CommonErrorDto body = CommonErrorDto.builder()
                .status_code(HttpStatus.UNAUTHORIZED.value())
                .status_message("유효하지 않은 토큰입니다.")
                .build();

        /// JSON으로 써서 반환
        new ObjectMapper().writeValue(response.getWriter(), body);
        response.getWriter().flush();
    }
}
