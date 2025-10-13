package com.careup.ordering.common.auth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BranchTokenIntrospectionClient {

    private final RestClient rest;

    public BranchTokenIntrospectionClient(
            @Value("${careup.branch.base-url:http://localhost:8082}") String baseUrl
    ) {
        // 타임아웃 기본 설정(네트워크 이슈시 지연 방지)
        var reqFactory = new SimpleClientHttpRequestFactory();
        reqFactory.setConnectTimeout(2000);
        reqFactory.setReadTimeout(2000);

        this.rest = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(reqFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public IntrospectionResult introspect(@NotBlank String accessToken) {
        return rest.post()
                .uri("/auth/introspect")
                .body(new IntrospectionRequest(accessToken))
                .retrieve()
                .body(IntrospectionResult.class);
    }

    public record IntrospectionRequest(String token) {}
    public record IntrospectionResult(
            boolean active,
            String role,
            Long employeeId,
            Long iat,
            Long exp
    ) {}
}
