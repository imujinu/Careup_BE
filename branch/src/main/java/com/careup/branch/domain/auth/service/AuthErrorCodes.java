package com.careup.branch.domain.auth.service;

public final class AuthErrorCodes {
    private AuthErrorCodes() {}

    public static final String ID_FORMAT_INVALID  = "ID_FORMAT_INVALID";
    public static final String PWD_FORMAT_INVALID = "PWD_FORMAT_INVALID";

    public static final String EMAIL_NOT_FOUND    = "EMAIL_NOT_FOUND";
    public static final String MOBILE_NOT_FOUND   = "MOBILE_NOT_FOUND";
    public static final String PASSWORD_MISMATCH  = "PASSWORD_MISMATCH";
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";

    public static final String ACCOUNT_LOCKED     = "ACCOUNT_LOCKED";
    public static final String ACCOUNT_INACTIVE   = "ACCOUNT_INACTIVE";

    public static final String REFRESH_TOKEN_REQUIRED = "REFRESH_TOKEN_REQUIRED";
}
