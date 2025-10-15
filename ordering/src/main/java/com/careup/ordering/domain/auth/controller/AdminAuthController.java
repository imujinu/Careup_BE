package com.careup.ordering.domain.auth.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.common.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 임시 관리자 로그인 컨트롤러
 * 관리자 로그인이 필요함.
 * 실제 운영에서는 Member 서비스의 Employee 인증을 사용해야 합니다.
 * 현재는 개발/테스트용으로 간단한 관리자 토큰 발급 기능만 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/auth/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 임시 관리자 로그인 (개발/테스트용)
     * 
     * POST /auth/admin/login
     * {
     *   "username": "admin",
     *   "password": "admin1234"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<CommonSuccessDto> adminLogin(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        log.info("관리자 로그인 시도: {}", username);

        // 임시 하드코딩된 관리자 계정
        if ("admin".equals(username) && "admin1234".equals(password)) {
            // 임시 관리자 ID (실제로는 DB에서 조회)
            Long adminId = 999L;
            
            // 관리자 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(adminId, "ROLE_HQ_ADMIN");
            String refreshToken = jwtTokenProvider.createRefreshToken(adminId, true);

            Map<String, Object> result = new HashMap<>();
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("memberId", adminId);
            result.put("role", "HQ_ADMIN");
            result.put("username", username);

            log.info("관리자 로그인 성공: {}", username);

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(result)
                            .status_code(HttpStatus.OK.value())
                            .status_message("관리자 로그인 성공 (임시)")
                            .build()
            );
        }

        // 실패 시
        log.warn("관리자 로그인 실패: 잘못된 인증 정보");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                CommonSuccessDto.builder()
                        .status_code(HttpStatus.UNAUTHORIZED.value())
                        .status_message("관리자 인증 실패")
                        .build()
        );
    }

    /**
     * 다양한 권한 테스트용 로그인
     */
    @PostMapping("/login/{role}")
    public ResponseEntity<CommonSuccessDto> adminLoginByRole(
            @PathVariable String role,
            @RequestBody Map<String, String> request) {
        
        String username = request.get("username");
        String password = request.get("password");

        if (!"admin1234".equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    CommonSuccessDto.builder()
                            .status_code(HttpStatus.UNAUTHORIZED.value())
                            .status_message("인증 실패")
                            .build()
            );
        }

        Long adminId = 999L;
        String roleWithPrefix = "ROLE_" + role.toUpperCase();
        
        String accessToken = jwtTokenProvider.createAccessToken(adminId, roleWithPrefix);
        String refreshToken = jwtTokenProvider.createRefreshToken(adminId, true);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("memberId", adminId);
        result.put("role", role.toUpperCase());
        result.put("username", username);

        log.info("관리자 로그인 성공 - Role: {}", role);

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("관리자 로그인 성공 (" + role + ")")
                        .build()
        );
    }
}
