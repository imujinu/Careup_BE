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
import java.util.Date;
import java.util.List;
import java.util.Map;

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
                tryAuthenticateBranchEmployee(token);
            }
        }

        chain.doFilter(request, response);
    }

    private boolean tryAuthenticateOrderingCustomer(String token, HttpServletResponse response) throws IOException {
        try {
            Claims c = jwt.parseAccessToken(token);

            Date iat = c.getIssuedAt();
            long iatMs = (iat != null) ? iat.getTime() : 0L;

            Long memberId = Long.valueOf(String.valueOf(c.get("memberId")));
            if (memberId != null && customerRevokeStore.isTokenObsolete(memberId, iatMs)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"status_code\":401,\"status_message\":\"세션이 만료되었습니다(보안 변경 적용). 다시 로그인하세요.\"}");
                return false;
            }

            String role = (c.get("role") == null || String.valueOf(c.get("role")).isBlank())
                    ? "CUSTOMER" : String.valueOf(c.get("role"));
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

            var auth = new UsernamePasswordAuthenticationToken(memberId, null, authorities);
            auth.setDetails(c);
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
            if (res != null && res.active() && res.employeeId() != null && res.role() != null) {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + res.role()));
                var auth = new UsernamePasswordAuthenticationToken(res.employeeId(), null, authorities);
                auth.setDetails(Map.of(
                        "realm", "EMP",
                        "employeeId", res.employeeId(),
                        "role", res.role(),
                        "iat", res.iat(),
                        "exp", res.exp()
                ));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            log.debug("[JWT][ORDERING] branch introspection failed: {}", e.getMessage());
        }
    }
}
