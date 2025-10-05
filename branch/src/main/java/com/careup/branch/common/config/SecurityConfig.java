package com.careup.branch.common.config;

import com.careup.branch.common.auth.JwtAccessDeniedHandler;        /// 403 핸들러
import com.careup.branch.common.auth.JwtAuthenticationEntryPoint;   /// 401 핸들러
import com.careup.branch.common.auth.JwtTokenFilter;                /// JWT 필터

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; /// @PreAuthorize 사용
import org.springframework.security.config.annotation.web.builders.HttpSecurity;                 /// 보안 설정 DSL
import org.springframework.security.config.http.SessionCreationPolicy;                           /// 세션 정책(STATe less)
import org.springframework.security.crypto.factory.PasswordEncoderFactories;                     /// 비밀번호 인코더 팩토리
import org.springframework.security.crypto.password.PasswordEncoder;                             /// 비밀번호 인코더
import org.springframework.security.web.SecurityFilterChain;                                     /// 필터체인 빈
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;     /// 필터 위치 기준

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /// 시큐리티 필터 체인(실제 보안 규칙을 담은 객체)을 스프링 빈으로 등록
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtTokenFilter jwtTokenFilter,
                                                   JwtAuthenticationEntryPoint entryPoint,
                                                   JwtAccessDeniedHandler deniedHandler) throws Exception {
        /// 1) CSRF 비활성화(세션/폼로그인 안 쓸 API 서버라면 보통 끔)
        http.csrf(csrf -> csrf.disable());

        /// 1-1) 기본 인증(브라우저 팝업) 비활성화
        http.httpBasic(b -> b.disable());

        /// 1-2) 세션을 만들지 않도록(완전 무상태) 설정
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        /// 2) URL 접근 권한 설정
        http.authorizeHttpRequests(auth -> auth
                /// 로그인 없이 접근 가능한 공개 경로들
                .requestMatchers("/actuator/**", "/auth/**", "/public/**").permitAll()
                /// 그 외 모든 경로는 인증(토큰) 필요
                .anyRequest().authenticated()
        );

        /// 3) 인증 실패(401) / 권한 실패(403) 핸들러 연결
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(deniedHandler)
        );

        /// 4) JWT 필터를 UsernamePasswordAuthenticationFilter "앞에" 두기
        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        /// 5) 최종 빌드
        return http.build();
    }

    /// 비밀번호를 해싱/검증하기 위한 인코더(BCrypt 기본)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
