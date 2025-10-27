package com.careup.ordering.common.auth;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @Min(1)
    private int accessTokenExpiryMinutes;

    @Min(1)
    private int refreshTokenExpiryDaysPersistent;

    @Min(1)
    private int refreshTokenExpiryDaysNonPersistent;

    @NotBlank
    private String secretKeyAt;

    @NotBlank
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
