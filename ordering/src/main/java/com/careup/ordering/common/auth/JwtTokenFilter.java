package com.careup.ordering.common.auth;

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

    private final JwtTokenProvider jwt;

    public JwtTokenFilter(JwtTokenProvider jwt) {
        this.jwt = jwt;
    }

    /// 매 요청마다 AT를 검증하여 SecurityContext에 인증정보 저장
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearer != null && bearer.startsWith("Bearer ")) {
            try {
                String token = bearer.substring(7);
                Claims c = jwt.parseAccessToken(token);

                // role 미포함 시 기본 CUSTOMER
                String role = String.valueOf(c.get("role") == null ? "CUSTOMER" : c.get("role"));
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                // principal은 고객 memberId (클레임 'id')
                var auth = new UsernamePasswordAuthenticationToken(c.get("id"), null, authorities);
                auth.setDetails(c);

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                log.debug("[JWT] token parse/verify failed: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}
