// src/main/java/com/careup/branch/domain/auth/utils/LoginValidators.java
package com.careup.branch.domain.auth.utils;

import java.util.regex.Pattern;

public final class LoginValidators {
    private LoginValidators() {}

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // 하이픈/공백 등 비숫자 문자를 제거한 뒤 검사: 010/011/016/017/018/019 + 7~8자리
    private static final Pattern PHONE_DIGITS =
            Pattern.compile("^01[016789]\\d{7,8}$");

    /** 이메일 또는 휴대폰(하이픈 유무 무관) 유효성 검사 */
    public static boolean isValidId(String id) {
        if (id == null) return false;
        String v = id.trim();
        if (v.isEmpty()) return false;

        // 이메일 형식 먼저 체크
        if (EMAIL.matcher(v).matches()) return true;

        // 휴대폰: 비숫자 제거 후 숫자 패턴 체크
        String digits = v.replaceAll("\\D", "");
        return PHONE_DIGITS.matcher(digits).matches();
    }

    public static boolean isValidPassword(String pw) {
        if (pw == null) return false;
        return pw.trim().length() >= 4; // 정책에 따라 조정
    }

    public static boolean looksLikeEmail(String id) {
        return id != null && id.contains("@");
    }
}
