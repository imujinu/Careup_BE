package com.careup.branch.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntrospectResponse {
    private boolean active;
    private Long employeeId;
    private Long branchId;
    private String role;
    private Long iat; // seconds
    private Long exp; // seconds
    private String email; // NEW
}
