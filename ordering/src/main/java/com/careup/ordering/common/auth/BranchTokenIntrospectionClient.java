package com.careup.ordering.common.auth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BranchTokenIntrospectionClient {

    private final RestClient rest;

    public BranchTokenIntrospectionClient(
            RestClient.Builder lb,                                   // ★ LoadBalanced 주입
            @Value("${careup.branch.service-id:branch-service}") String serviceId
    ) {
        this.rest = lb.baseUrl("http://" + serviceId).build();       // ★ 서비스명으로 호출
    }

    public IntrospectionResult introspect(@NotBlank String accessToken) {
        return rest.post()
                .uri("/auth/introspect")
                .body(new IntrospectionRequest(accessToken))
                .retrieve()
                .body(IntrospectionResult.class);
    }

    public record IntrospectionRequest(String token) {}
    public record IntrospectionResult(boolean active, String role, Long employeeId, Long branchId, Long iat, Long exp, String email) {}
}
