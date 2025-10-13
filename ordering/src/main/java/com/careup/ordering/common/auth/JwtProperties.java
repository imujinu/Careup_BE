package com.careup.ordering.common.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private int accessTokenExpiryMinutes;             /// AT 만료(분)
    private int refreshTokenExpiryDaysPersistent;     /// RT 만료(일, 자동로그인 ON)
    private int refreshTokenExpiryDaysNonPersistent;  /// RT 만료(일, 자동로그인 OFF)
    private String secretKeyAt;                       /// AT 서명키(Base64, HS512)
    private String secretKeyRt;                       /// RT 서명키(Base64, HS512)

    public int getAccessTokenExpiryMinutes() { return accessTokenExpiryMinutes; }
    public void setAccessTokenExpiryMinutes(int v) { this.accessTokenExpiryMinutes = v; }

    public int getRefreshTokenExpiryDaysPersistent() { return refreshTokenExpiryDaysPersistent; }
    public void setRefreshTokenExpiryDaysPersistent(int v) { this.refreshTokenExpiryDaysPersistent = v; }

    public int getRefreshTokenExpiryDaysNonPersistent() { return refreshTokenExpiryDaysNonPersistent; }
    public void setRefreshTokenExpiryDaysNonPersistent(int v) { this.refreshTokenExpiryDaysNonPersistent = v; }

    public String getSecretKeyAt() { return secretKeyAt; }
    public void setSecretKeyAt(String v) { this.secretKeyAt = v; }

    public String getSecretKeyRt() { return secretKeyRt; }
    public void setSecretKeyRt(String v) { this.secretKeyRt = v; }
}
