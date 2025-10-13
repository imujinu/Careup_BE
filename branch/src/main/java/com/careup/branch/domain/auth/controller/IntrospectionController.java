package com.careup.branch.domain.auth.controller;

import com.careup.branch.common.auth.JwtTokenProvider;
import com.careup.branch.domain.auth.dto.request.IntrospectRequest;
import com.careup.branch.domain.auth.dto.response.IntrospectResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class IntrospectionController {

    private final JwtTokenProvider jwt;

    @PostMapping(value = "/auth/introspect",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public IntrospectResponse introspect(@RequestBody IntrospectRequest req) {
        try {
            Claims c = jwt.parseAccessToken(req.getToken());
            String role = String.valueOf(c.get("role"));
            Long employeeId = Long.valueOf(String.valueOf(c.get("employeeId")));
            long iat = c.getIssuedAt() != null ? c.getIssuedAt().toInstant().getEpochSecond() : 0L;
            long exp = c.getExpiration() != null ? c.getExpiration().toInstant().getEpochSecond() : 0L;

            return IntrospectResponse.builder()
                    .active(true)
                    .role(role)
                    .employeeId(employeeId)
                    .iat(iat)
                    .exp(exp)
                    .build();
        } catch (Exception e) {
            return IntrospectResponse.builder().active(false).build();
        }
    }
}
