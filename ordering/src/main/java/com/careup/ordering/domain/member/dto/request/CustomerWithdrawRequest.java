package com.careup.ordering.domain.member.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객 회원탈퇴 요청 (소프트 삭제)
 * - 일반 계정: currentPassword 필수
 * - 소셜 연동 보유 계정: 비밀번호 없이도 가능
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerWithdrawRequest {

    /** 일반 계정은 필수, 소셜 연동 계정은 선택 */
    private String currentPassword;

    /** 사용자가 탈퇴 진행에 동의했음을 명시적으로 표시 */
    @AssertTrue(message = "탈퇴 진행에 대한 동의가 필요합니다.")
    private boolean agree;
}
