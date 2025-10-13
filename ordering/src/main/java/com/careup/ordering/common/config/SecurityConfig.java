package com.careup.ordering.common.config;

import com.careup.ordering.common.auth.JwtAccessDeniedHandler;
import com.careup.ordering.common.auth.JwtAuthenticationEntryPoint;
import com.careup.ordering.common.auth.JwtTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                .requestMatchers(
                        "/actuator/**",
                        "/public/**",
                        "/health",
                        "/products/**"
                ).permitAll()

                .requestMatchers(
                        "/auth/customer/signup",
                        "/auth/customer/login",
                        "/auth/customer/refresh",
                        "/auth/customer/logout",
                        "/auth/customer/password/forgot",
                        "/auth/customer/password/reset"
                ).permitAll()

                .requestMatchers(
                        "/cart/**", "/orders/**", "/customers/**"
                ).hasRole("CUSTOMER")

                .requestMatchers(
                        "/admin/**", "/management/**", "/product-admin/**"
                ).hasAnyRole("HQ_ADMIN","BRANCH_ADMIN","FRANCHISE_OWNER")

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
