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
        http.cors(cors -> {});

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/public/**", "/health").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 고객 인증 플로우 + OAuth 엔드포인트 공개
                .requestMatchers(
                        "/auth/customers/signup",
                        "/auth/customers/login",
                        "/auth/customers/refresh",
                        "/auth/customers/logout",
                        "/auth/customers/password/forgot",
                        "/auth/customers/password/reset",
                        "/auth/customers/oauth/google",
                        "/auth/customers/oauth/kakao",
                        "/auth/customers/oauth/update"
                ).permitAll()

                .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/products/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/categories/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/categories/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/categories/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                .requestMatchers("/inventory/flow").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers("/inventory/adjustment-history").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers("/inventory/branch/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers("/inventory/safety-stock").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers("/inventory/adjust").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers("/inventory/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER","STAFF")

                .requestMatchers(HttpMethod.GET, "/coupons/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.PUT, "/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")
                .requestMatchers(HttpMethod.DELETE, "/coupons/**").hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

                .requestMatchers("/cart/**", "/orders/**", "/customers/**").hasRole("CUSTOMER")

                .requestMatchers("/admin/**", "/management/**", "/product-admin/**")
                .hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

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
