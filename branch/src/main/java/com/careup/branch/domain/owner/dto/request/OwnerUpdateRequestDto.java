package com.careup.branch.domain.owner.dto.request;

import com.careup.branch.domain.employee.entity.AuthorityType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OwnerUpdateRequestDto {

    @NotNull(message = "권한 타입은 필수입니다.")
    private AuthorityType authorityType; // BRANCH_ADMIN 또는 FRANCHISE_OWNER

    @NotNull(message = "지점 ID는 필수입니다.")
    private Long branchId;
}

