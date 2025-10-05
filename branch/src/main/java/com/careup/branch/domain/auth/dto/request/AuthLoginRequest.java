package com.careup.branch.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthLoginRequest {
    private String id;           /// 이메일 또는 휴대폰 번호
    private String password;     /// 평문 비밀번호
    private boolean rememberMe;  /// RT 만료전략(자동로그인)
}
