package com.careup.branch.common.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties (prefix = "jwt")
public class JwtProperties {

    private int accessTokenExpiryMinutes; /// Access Token 만료 시간(분 단위)
    private int refreshTokenExpiryDaysPersistent; /// Refresh Token 만료 기간(일 단위, 자동 로그인 ON일 경우)
    private int refreshTokenExpiryDaysNonPersistent; /// Refresh Token 만료 기간(일 단위, 자동 로그인 OFF일 경우)

    private String secretKeyAt; /// Access Token을 만들 때 서명에 쓰는 비밀키(Base64)
    private String secretKeyRt; /// Refresh Token을 만들 때 서명에 쓰는 비밀키(Base64)

    /// 위에 적은 accessTokenExpiryMinutes 값을 코드에서 읽을 때 사용
    public int getAccessTokenExpiryMinutes()
        { return accessTokenExpiryMinutes; }
    /// 스프링이 yml에 적힌 값을 여기에 넣어줄 때 사용
    public void setAccessTokenExpiryMinutes(int accessTokenExpiryMinutes)
        { this.accessTokenExpiryMinutes = accessTokenExpiryMinutes; }

    public int getRefreshTokenExpiryDaysPersistent()
        { return refreshTokenExpiryDaysPersistent; }
    public void setRefreshTokenExpiryDaysPersistent(int refreshTokenExpiryDaysPersistent)
        { this.refreshTokenExpiryDaysPersistent = refreshTokenExpiryDaysPersistent; }

    public int getRefreshTokenExpiryDaysNonPersistent()
        { return refreshTokenExpiryDaysNonPersistent; }
    public void setRefreshTokenExpiryDaysNonPersistent(int refreshTokenExpiryDaysNonPersistent)
        { this.refreshTokenExpiryDaysNonPersistent = refreshTokenExpiryDaysNonPersistent; }

    /// AT 서명 비밀키(Base64)를 코드에서 읽을 때 사용
    public String getSecretKeyAt() { return secretKeyAt; }
    /// 스프링이 yml의 secretKeyAt 값을 넣어줄 때 사용
    public void setSecretKeyAt(String secretKeyAt) { this.secretKeyAt = secretKeyAt; }

    /// RT 서명 비밀키(Base64)를 코드에서 읽을 때 사용
    public String getSecretKeyRt() { return secretKeyRt; }
    /// 스프링이 yml의 secretKeyRt 값을 넣어줄 때 사용
    public void setSecretKeyRt(String secretKeyRt) { this.secretKeyRt = secretKeyRt; }
}
