package com.careup.branch.domain.branch.dto.branch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSimpleDto {
    private Long id;
    private String name;
    private String address;
    private String addressDetail;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String email;
}

