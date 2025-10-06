package com.careup.branch.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthLoginResponse {
    private String tokenType;        /// "Bearer"
    private String accessToken;
    private String refreshToken;
    private int expiresInMinutes;    /// AT 만료(분)
    private String role;             /// HQ_ADMIN / BRANCH_ADMIN / FRANCHISE_OWNER / STAFF
    private Long employeeId;

    private String name;             /// 성명
    private String title;            /// 직급(권한명 그대로 노출하거나 별도 매핑 가능)
    private String email;            /// 이메일
    private String mobile;           /// 휴대폰

    private Long branchId;           /// 현재 배치 지점 ID (없으면 null)
    private String branchName;       /// 현재 배치 지점명 (없으면 null)
}
