package com.careup.branch.domain.branch.dto.branch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchUpdateRequestDto {
    private String name;
    private String profileImageUrl;
}
