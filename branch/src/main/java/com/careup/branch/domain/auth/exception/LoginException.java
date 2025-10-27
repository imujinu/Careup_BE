// src/main/java/com/careup/branch/domain/auth/exception/LoginException.java
package com.careup.branch.domain.auth.exception;

import lombok.Getter;

@Getter
public class LoginException extends RuntimeException {
    private final int status;
    private final String code;

    private LoginException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static LoginException idFormatInvalid(String message) {
        return new LoginException(400, "ID_FORMAT_INVALID", message);
    }

    public static LoginException pwdFormatInvalid(String message) {
        return new LoginException(400, "PWD_FORMAT_INVALID", message);
    }

    public static LoginException emailNotFound() {
        return new LoginException(401, "EMAIL_NOT_FOUND", "등록되지 않은 이메일입니다.");
    }

    public static LoginException mobileNotFound() {
        return new LoginException(401, "MOBILE_NOT_FOUND", "등록되지 않은 휴대폰 번호입니다.");
    }

    public static LoginException passwordMismatch() {
        return new LoginException(401, "PASSWORD_MISMATCH", "잘못된 비밀번호입니다.");
    }

    public static LoginException accountInactive() {
        return new LoginException(403, "ACCOUNT_INACTIVE", "비활성화된 계정입니다.");
    }

    public static LoginException accountLocked() {
        return new LoginException(403, "ACCOUNT_LOCKED", "계정이 잠겨 있습니다.");
    }
}
