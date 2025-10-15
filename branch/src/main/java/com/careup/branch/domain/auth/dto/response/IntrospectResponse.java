package com.careup.branch.domain.auth.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntrospectResponse {
    private boolean active;
    private Long employeeId;
    private String role;
    private Long iat; // seconds
    private Long exp; // seconds
}
