package com.careup.ordering.common.auth;

import com.careup.ordering.common.dto.CommonErrorDto;
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

    /// 인증 실패(401) 발생 시 호출
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        CommonErrorDto body = CommonErrorDto.builder()
                .status_code(HttpStatus.UNAUTHORIZED.value())
                .status_message("유효하지 않은 토큰입니다.")
                .build();

        new ObjectMapper().writeValue(response.getWriter(), body);
        response.getWriter().flush();
    }
}
