package com.careup.ordering.common.config;

import com.careup.ordering.common.auth.JwtAccessDeniedHandler;
import com.careup.ordering.common.auth.JwtAuthenticationEntryPoint;
import com.careup.ordering.common.auth.JwtTokenFilter;
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
                // 공용 공개
                .requestMatchers("/actuator/**", "/public/**", "/health").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 고객 인증 플로우 공개
                .requestMatchers(
                        "/auth/customers/signup",
                        "/auth/customers/login",
                        "/auth/customers/refresh",
                        "/auth/customers/logout",
                        "/auth/customers/password/forgot",
                        "/auth/customers/password/reset"
                ).permitAll()

                // 관리자 인증 등록좀 하려고 만듬
                .requestMatchers("/auth/admin/**").permitAll()

                // 상품/카테고리: 조회는 공개, 쓰기/수정/삭제는 관리자 계열만
                .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/categories/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/categories/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/categories/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                // 인벤토리: 전부 관리자 계열 전용 (주문/고객용 서비스에서 비공개)
                .requestMatchers("/inventory/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER","STAFF")

                // 쿠폰(가정): 조회는 공개, 생성/수정/삭제는 관리자
                .requestMatchers(HttpMethod.GET, "/coupons/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                // 고객 전용 영역
                .requestMatchers("/cart/**", "/orders/**", "/customers/**").hasRole("CUSTOMER")

                // 관리자 전용 영역(추가 관리용 라우트)
                .requestMatchers("/admin/**", "/management/**", "/product-admin/**")
                .hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                // 나머지는 로그인만 필요
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
