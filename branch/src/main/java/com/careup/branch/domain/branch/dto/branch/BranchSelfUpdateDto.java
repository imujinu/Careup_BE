package com.careup.branch.domain.branch.dto.branch;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 지점 자기수정 요청 DTO
 * - 가맹점주/직영점주가 요청 가능한 필드만 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSelfUpdateDto {

    /** 변경 희망 지점명 (선택) */
    @Size(max = 100, message = "지점명은 최대 100자까지 가능합니다.")
    private String name;

    /** 변경 희망 프로필 이미지 URL (선택) */
    @Size(max = 2000, message = "프로필 이미지 URL이 너무 깁니다.")
    private String profileImageUrl;
}

