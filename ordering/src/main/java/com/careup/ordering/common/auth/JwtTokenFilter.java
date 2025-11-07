package com.careup.ordering.common.auth;

import com.careup.ordering.common.dto.CommonErrorDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final BranchTokenIntrospectionClient branchIntrospectionClient;
    private final CustomerRevokeStore customerRevokeStore;

    public JwtTokenFilter(JwtTokenProvider jwt,
                          BranchTokenIntrospectionClient branchIntrospectionClient,
                          CustomerRevokeStore customerRevokeStore) {
        this.jwt = jwt;
        this.branchIntrospectionClient = branchIntrospectionClient;
        this.customerRevokeStore = customerRevokeStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String bearer = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);
            boolean ok = tryAuthenticateOrderingCustomer(token, response);
            if (!ok) {
                if (response.isCommitted()) return;
                tryAuthenticateBranchEmployee(token);
            }
        }
        if (response.isCommitted()) return;
        chain.doFilter(request, response);
    }

    private boolean tryAuthenticateOrderingCustomer(String token, HttpServletResponse response) throws IOException {
        try {
            Claims c = jwt.parseAccessToken(token);
            Date iat = c.getIssuedAt();
            long iatMs = (iat != null) ? iat.getTime() : 0L;

            Object mid = c.get("memberId");
            if (mid == null) return false;
            Long memberId = Long.valueOf(String.valueOf(mid));

            if (customerRevokeStore.isTokenObsolete(memberId, iatMs)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                CommonErrorDto body = CommonErrorDto.builder()
                        .status_code(HttpServletResponse.SC_UNAUTHORIZED)
                        .status_message("세션이 만료되었습니다(보안 변경 적용). 다시 로그인하세요.")
                        .build();
                new ObjectMapper().writeValue(response.getWriter(), body);
                response.getWriter().flush();
                return false;
            }

            String roleRaw = c.get("role") == null ? "" : String.valueOf(c.get("role"));
            String role = roleRaw.isBlank() ? "CUSTOMER" : (roleRaw.startsWith("ROLE_") ? roleRaw.substring(5) : roleRaw);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

            var auth = new UsernamePasswordAuthenticationToken(memberId, token, authorities);
            auth.setDetails(Map.of("realm", "CUS", "claims", c));
            SecurityContextHolder.getContext().setAuthentication(auth);
            return true;
        } catch (Exception e) {
            log.debug("[JWT][ORDERING] customer token parse failed: {}", e.getMessage());
            return false;
        }
    }

    private void tryAuthenticateBranchEmployee(String token) {
        try {
            var res = branchIntrospectionClient.introspect(token);
            if (res == null || !res.active() || res.employeeId() == null || res.role() == null || res.role().isBlank()) return;

            String roleNormalized = res.role().startsWith("ROLE_") ? res.role() : "ROLE_" + res.role();
            var authorities = List.of(new SimpleGrantedAuthority(roleNormalized));

            var auth = new UsernamePasswordAuthenticationToken(res.employeeId(), token, authorities);
            auth.setDetails(Map.of(
                    "realm", "EMP",
                    "employeeId", res.employeeId(),
                    "branchId", res.branchId(),
                    "role", roleNormalized,
                    "iat", res.iat(),
                    "exp", res.exp()
            ));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            log.debug("[JWT][ORDERING] branch introspection failed: {}", e.getMessage());
        }
    }
}
