package com.careup.ordering.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    /// 고객 전용 영역(Realm)
    public enum AuthRealm { CUS }

    private final JwtProperties props;
    private final RedisTemplate<String, String> redis;

    private Key atKey;
    private Key rtKey;

    public JwtTokenProvider(JwtProperties props,
                            @Qualifier("rtInventory") RedisTemplate<String, String> redis) {
        this.props = props;
        this.redis = redis;
    }

    @PostConstruct
    public void init() {
        byte[] atBytes = Base64.getDecoder().decode(props.getSecretKeyAt());
        byte[] rtBytes = Base64.getDecoder().decode(props.getSecretKeyRt());
        if (atBytes.length < 64) throw new IllegalArgumentException("HS512용 AT 키는 64바이트 이상이어야 합니다.");
        if (rtBytes.length < 64) throw new IllegalArgumentException("HS512용 RT 키는 64바이트 이상이어야 합니다.");
        this.atKey = Keys.hmacShaKeyFor(atBytes);
        this.rtKey = Keys.hmacShaKeyFor(rtBytes);
    }

    private static String rtKey(AuthRealm realm, Long id) {
        return "RT:" + realm.name() + ":" + id; // RT:CUS:{memberId}
    }

    /** Access Token 생성 (sub = "CUS:{memberId}") */
    public String createAccessToken(AuthRealm realm, Long memberId, String role) {
        Date now = new Date();
        long atMillis = Duration.ofMinutes(props.getAccessTokenExpiryMinutes()).toMillis();

        Claims claims = Jwts.claims().setSubject(realm.name() + ":" + memberId);
        claims.put("realm", realm.name()); // CUS
        claims.put("id", memberId);        // 고객 ID
        claims.put("role", role);          // CUSTOMER

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + atMillis))
                .signWith(atKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /** rememberMe=false면 RT 미발급(null) */
    public String createRefreshToken(AuthRealm realm, Long memberId, boolean rememberMe) {
        if (!rememberMe) return null;

        Date now = new Date();
        long rtMillis = Duration.ofDays(props.getRefreshTokenExpiryDaysPersistent()).toMillis();

        String rt = Jwts.builder()
                .setSubject(realm.name() + ":" + memberId) // "CUS:{id}"
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + rtMillis))
                .signWith(rtKey, SignatureAlgorithm.HS512)
                .compact();

        // 계정 당 단일 슬롯
        redis.opsForValue().set(rtKey(realm, memberId), rt, Duration.ofMillis(rtMillis));
        return rt;
    }

    /** AT 서명/만료 검증 + 클레임 */
    public Claims parseAccessToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(atKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** RT 서명/만료 + Redis 저장본 일치 검증 */
    public Claims validateRefreshToken(String refreshToken) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(rtKey)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        String[] parts = claims.getSubject().split(":"); // CUS:{id}
        if (parts.length != 2 || !"CUS".equals(parts[0])) {
            throw new JwtException("유효하지 않은 토큰입니다.");
        }
        Long memberId = Long.valueOf(parts[1]);

        String saved = redis.opsForValue().get(rtKey(AuthRealm.CUS, memberId));
        if (saved == null || !saved.equals(refreshToken)) {
            throw new JwtException("유효하지 않은 토큰입니다.");
        }
        return claims;
    }

    /** 로그아웃(고객 단위) → RT 폐기 */
    public void revokeRefreshToken(Long memberId) {
        redis.delete(rtKey(AuthRealm.CUS, memberId));
    }
}
