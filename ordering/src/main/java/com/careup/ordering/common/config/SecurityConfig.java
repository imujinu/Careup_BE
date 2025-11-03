package com.careup.ordering.common.config;

import com.careup.ordering.common.auth.JwtAccessDeniedHandler;
import com.careup.ordering.common.auth.JwtAuthenticationEntryPoint;
import com.careup.ordering.common.auth.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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

        http.csrf(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 백엔드 CORS 비활성화 (게이트웨이에서만 CORS 처리)
        http.cors(AbstractHttpConfigurer::disable);

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/public/**", "/health").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 고객 인증 + OAuth 공개
                .requestMatchers(
                        "/auth/customers/signup",
                        "/auth/customers/login",
                        "/auth/customers/refresh",
                        "/auth/customers/logout",
                        "/auth/customers/password/forgot",
                        "/auth/customers/password/reset",
                        "/auth/customers/oauth/state",
                        "/auth/customers/oauth/google",
                        "/auth/customers/oauth/kakao",
                        "/auth/customers/oauth/update"
                ).permitAll()

                // 상품/카테고리: 조회는 공개, 쓰기/수정/삭제는 관리자 계열만
                .requestMatchers(HttpMethod.GET, "/products/**", "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/products/**", "/api/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/products/**", "/api/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/products/**", "/api/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                // 상품 관련 - GET은 공개, 나머지는 관리자 전용
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/public/products").permitAll()  // 고객용 상품 목록 공개
                .requestMatchers(HttpMethod.GET, "/api/public/products/with-branches").permitAll()  // 고객용 상품 목록 (지점 정보 포함)
                .requestMatchers(HttpMethod.GET, "/api/public/products/search").permitAll()  // 고객용 상품 검색
                .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                // 카테고리 - GET은 공개, 나머지는 관리자 전용
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                // 프로모션 - GET은 공개, 나머지는 관리자 전용
                .requestMatchers(HttpMethod.GET, "/api/promotions/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/promotions/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/promotions/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/promotions/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                // 상품 문의 - GET은 공개, POST/PUT/DELETE는 CUSTOMER 전용
                .requestMatchers(HttpMethod.GET, "/api/product-inquiries/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/product-inquiries/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/api/product-inquiries/**").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.DELETE, "/api/product-inquiries/**").hasRole("CUSTOMER")

                // 재고 조회 - GET은 공개, 나머지는 관리자 전용
                .requestMatchers(HttpMethod.GET, "/inventory/branch-products/**").permitAll()
                // 인벤토리: 본사/가맹점/직원 권한 허용 (STAFF 포함)
                .requestMatchers("/inventory/flow").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER","STAFF")
                .requestMatchers("/inventory/adjustment-history").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER","STAFF")
                .requestMatchers("/inventory/branch/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER","STAFF")
                .requestMatchers("/inventory/safety-stock").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER","STAFF")
                .requestMatchers("/inventory/adjust").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER","STAFF")
                // 나머지 인벤토리 API는 기존 권한 유지
                .requestMatchers("/inventory/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER","STAFF")

                // 쿠폰(가정): 조회는 공개, 생성/수정/삭제는 관리자
                .requestMatchers(HttpMethod.GET, "/coupons/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                // 쿠폰 - GET은 공개, 나머지는 관리자 전용
                .requestMatchers(HttpMethod.GET, "/api/coupons/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/api/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                // 주문 조회 - 고객 + 관리자 + 일반직원(조회 허용)
                .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("CUSTOMER", "HQ_ADMIN", "BRANCH_ADMIN", "FRANCHISE_OWNER", "STAFF")

                // 주문 승인/거부 - 일반직원까지 허용(요구사항에 따라 확장)
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/approve").hasAnyRole("HQ_ADMIN", "BRANCH_ADMIN", "FRANCHISE_OWNER", "STAFF")
                .requestMatchers(HttpMethod.PUT, "/api/orders/*/reject").hasAnyRole("HQ_ADMIN", "BRANCH_ADMIN", "FRANCHISE_OWNER", "STAFF")

                // 주문 생성 - 고객 전용
                .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CUSTOMER")

                // 주문 취소 - 고객 전용
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("CUSTOMER")

                // 장바구니, 결제 - CUSTOMER 전용
                .requestMatchers("/api/cart/**", "/api/payments/**").hasRole("CUSTOMER")

                // 고객 관리, 단골 고객 - 관리자 전용
                .requestMatchers("/admin/**", "/management/**", "/product-admin/**", "/api/loyal-customers/**")
                .hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                // 매출 통계 조회 (MSA 간 통신용) - 관리자 전용
                .requestMatchers("/sales/**").authenticated()

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
