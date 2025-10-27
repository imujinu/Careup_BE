package com.careup.branch.common.auth;

import com.careup.branch.common.dto.CommonErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final ForceLogoutStore forceLogoutStore;

    /** 점진 도입 → true로 전환 시 이메일 클레임 없으면 401 */
    private final boolean requireEmailClaim;

    public JwtTokenFilter(
            JwtTokenProvider jwt,
            ForceLogoutStore forceLogoutStore,
            @Value("${security.jwt.require-email:false}") boolean requireEmailClaim
    ) {
        this.jwt = jwt;
        this.forceLogoutStore = forceLogoutStore;
        this.requireEmailClaim = requireEmailClaim;
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
                    write401(response, "세션이 만료되었습니다(보안 변경 적용). 다시 로그인하세요.");
                    return;
                }

                // 이메일 클레임 검증(토글)
                String email = c.get("email", String.class);
                if (requireEmailClaim && (email == null || email.isBlank())) {
                    log.debug("[JWT] 이메일 클레임 누락");
                    write401(response, "유효하지 않은 토큰입니다.");
                    return;
                } else if (email == null || email.isBlank()) {
                    // 점진 도입 단계: 경로 추적용
                    log.trace("[JWT] 이메일 클레임 없음(호환 단계)");
                }

                String role = String.valueOf(c.get("role"));
                if (role != null && !role.isBlank()) {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var auth = new UsernamePasswordAuthenticationToken(c.getSubject(), null, authorities);
                    auth.setDetails(c); // email 포함된 Claims 전달
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.debug("[JWT] token parse/verify failed: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private void write401(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        CommonErrorDto body = CommonErrorDto.builder()
                .status_code(HttpServletResponse.SC_UNAUTHORIZED)
                .status_message(message)
                .build();
        new ObjectMapper().writeValue(response.getWriter(), body);
        response.getWriter().flush();
    }
}
