package com.careup.branch.domain.auth.controller;

import com.careup.branch.common.auth.ForceLogoutStore;
import com.careup.branch.common.auth.JwtTokenProvider;
import com.careup.branch.domain.auth.dto.request.IntrospectRequest;
import com.careup.branch.domain.auth.dto.response.IntrospectResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequiredArgsConstructor
public class IntrospectionController {

    private final JwtTokenProvider jwt;
    private final ForceLogoutStore forceLogoutStore;

    @PostMapping(value = "/auth/introspect",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public IntrospectResponse introspect(@RequestBody IntrospectRequest req) {
        try {
            Claims c = jwt.parseAccessToken(req.getToken());
            Long employeeId = c.get("employeeId", Long.class);
            Date iat = c.getIssuedAt();
            long iatMs = (iat != null) ? iat.getTime() : 0L;

            if (employeeId == null || forceLogoutStore.isTokenObsolete(employeeId, iatMs)) {
                return IntrospectResponse.builder().active(false).build();
            }

            String role = String.valueOf(c.get("role"));
            long iatSec = (iat != null) ? iat.toInstant().getEpochSecond() : 0L;
            long expSec = (c.getExpiration() != null) ? c.getExpiration().toInstant().getEpochSecond() : 0L;

            return IntrospectResponse.builder()
                    .active(true)
                    .role(role)
                    .employeeId(employeeId)
                    .iat(iatSec)
                    .exp(expSec)
                    .build();
        } catch (Exception e) {
            return IntrospectResponse.builder().active(false).build();
        }
    }
}
