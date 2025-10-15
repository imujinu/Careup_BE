package com.careup.branch.common.config;

import com.careup.branch.common.auth.JwtAccessDeniedHandler;
import com.careup.branch.common.auth.JwtAuthenticationEntryPoint;
import com.careup.branch.common.auth.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtTokenFilter jwtTokenFilter,
                                                   JwtAuthenticationEntryPoint entryPoint,
                                                   JwtAccessDeniedHandler deniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.httpBasic(b -> b.disable());
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                // CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 공개 엔드포인트
                .requestMatchers(
                        "/actuator/**",
                        "/public/**",
                        "/health",
                        "/auth/login",
                        "/auth/refresh",
                        "/auth/logout",
                        "/auth/password/forgot",
                        "/auth/password/reset",
                        "/auth/introspect",            // Ordering 서버에서 토큰 검증용
                        "/auth/employees/id-lookup"    // 직원 아이디(이메일/휴대폰) 찾기
                ).permitAll()

                // 나머지는 인증 필요
                .anyRequest().authenticated()
        );

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(deniedHandler)
        );

        http.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
