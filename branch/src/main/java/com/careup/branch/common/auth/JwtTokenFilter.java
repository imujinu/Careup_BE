package com.careup.branch.common.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    /// 토큰 발급/검증 제공자(앞서 만든 JwtTokenProvider)
    private final JwtTokenProvider jwt;

    /// 생성자 주입
    public JwtTokenFilter(JwtTokenProvider jwt) {
        this.jwt = jwt;
    }

    /// 요청이 들어올 때마다 실행되는 메서드
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        /// 1) Authorization 헤더 추출
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);

        /// 2) Bearer 토큰 형식인지 확인
        if (bearer != null && bearer.startsWith("Bearer ")) {
            try {
                /// 3) "Bearer " 이후 실제 토큰만 잘라냄
                String token = bearer.substring(7);

                /// 4) 토큰 서명/만료 검증 + 클레임 추출
                Claims c = jwt.parseAccessToken(token);

                /// 5) 권한 문자열을 스프링 시큐리티 권한 객체로 변환
                String role = String.valueOf(c.get("role"));
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                /// 6) 인증 객체 생성(Principal: subject=employeeId 문자열)
                var auth = new UsernamePasswordAuthenticationToken(c.getSubject(), null, authorities);

                /// 7) 필요 시 세부정보로 Claims를 담아 컨트롤러에서 활용 가능
                auth.setDetails(c);

                /// 8) 컨텍스트에 저장 → 이후 @PreAuthorize 등에서 사용 가능
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                log.debug("[JWT] token parse/verify failed: {}", e.getMessage());
            }
        }

        /// 9) 다음 필터/컨트롤러로 진행
        chain.doFilter(request, response);
    }
}
