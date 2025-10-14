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
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final ForceLogoutStore forceLogoutStore;

    public JwtTokenFilter(JwtTokenProvider jwt, ForceLogoutStore forceLogoutStore) {
        this.jwt = jwt;
        this.forceLogoutStore = forceLogoutStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (bearer != null && bearer.startsWith("Bearer ")) {
            try {
                String token = bearer.substring(7);
                Claims c = jwt.parseAccessToken(token);

                Long employeeId = c.get("employeeId", Long.class);
                Date iat = c.getIssuedAt();
                long iatMs = (iat != null) ? iat.getTime() : 0L;

                if (employeeId != null && forceLogoutStore.isTokenObsolete(employeeId, iatMs)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status_code\":401,\"status_message\":\"세션이 만료되었습니다(보안 변경 적용). 다시 로그인하세요.\"}");
                    return;
                }

                String role = String.valueOf(c.get("role"));
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var auth = new UsernamePasswordAuthenticationToken(c.getSubject(), null, authorities);
                auth.setDetails(c);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                log.debug("[JWT] token parse/verify failed: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}
