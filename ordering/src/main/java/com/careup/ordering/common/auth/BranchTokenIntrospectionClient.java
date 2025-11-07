package com.careup.ordering.common.auth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class BranchTokenIntrospectionClient {

    private final RestClient rest;

    public BranchTokenIntrospectionClient(
            @Qualifier("lbRestClientBuilder") RestClient.Builder lb,
            @Qualifier("plainRestClientBuilder") RestClient.Builder plain,
            @Value("${careup.branch.introspection.base-url:}") String baseUrl,
            @Value("${careup.branch.service-id:branch-service}") String serviceId
    ) {
        if (StringUtils.hasText(baseUrl)) {
            // ★ Ingress 경유: 예) https://api.careup.store/branch-service
            String normalized = baseUrl.replaceAll("/+$", "");
            this.rest = plain.baseUrl(normalized).build();
        } else {
            // ★ Service Discovery/LB 경유: 예) http://branch-service
            this.rest = lb.baseUrl("http://" + serviceId).build();
        }
    }

    public IntrospectionResult introspect(@NotBlank String accessToken) {
        return rest.post()
                .uri("/auth/introspect")  // Ingress base-url이 /branch-service 라서 OK
                .body(new IntrospectionRequest(accessToken))
                .retrieve()
                .body(IntrospectionResult.class);
    }

    public record IntrospectionRequest(String token) {}
    public record IntrospectionResult(
            boolean active, String role, Long employeeId, Long branchId, Long iat, Long exp, String email
    ) {}
}
