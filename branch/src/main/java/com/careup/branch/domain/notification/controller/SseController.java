package com.careup.branch.domain.notification.controller;

import com.careup.branch.common.auth.JwtTokenProvider;
import com.careup.branch.domain.notification.service.SseEmitterRegistry;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class SseController {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final JwtTokenProvider jwtTokenProvider;
    @GetMapping("/connect")
    public SseEmitter subscribe(@RequestParam String token) {
        Authentication auth = getAuth(token);
        System.out.println("검증완료======" +  auth);
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        String email = details.get("email").toString();

        SseEmitter sseEmitter = new SseEmitter(14400 * 60 * 1000L);
        sseEmitterRegistry.addSseEmitter(email, sseEmitter);

        try {
            sseEmitter.send(SseEmitter.event().name("connect").data("연결완료"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[sse][conn!]");
        return sseEmitter;
    }

    private UsernamePasswordAuthenticationToken getAuth(String token) {
        Claims claims = jwtTokenProvider.parseAccessToken(token);
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);

        // 수동으로 Authentication 생성 및 SecurityContext에 등록
        var auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);// JWT 파싱
        auth.setDetails(claims);
        return auth;
    }

    @GetMapping("/disconnect")
    public void unSubscribe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        sseEmitterRegistry.removeSseEmitter(email);
    }
}
