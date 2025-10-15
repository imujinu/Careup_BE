package com.careup.branch.common.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private int accessTokenExpiryMinutes;
    private int refreshTokenExpiryDaysPersistent;
    private int refreshTokenExpiryDaysNonPersistent;
    private String secretKeyAt;
    private String secretKeyRt;

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
