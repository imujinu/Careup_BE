package com.careup.branch.common.auth;

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

    private static String rtKey(Long employeeId) {
        return "RT:EMP:" + employeeId;
    }

    /** subject = employeeId 문자열 */
    public String createAccessToken(Long employeeId, String role) {
        Date now = new Date();
        long atMillis = Duration.ofMinutes(props.getAccessTokenExpiryMinutes()).toMillis();

        Claims claims = Jwts.claims().setSubject(String.valueOf(employeeId));
        claims.put("role", role);
        claims.put("employeeId", employeeId);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + atMillis))
                .signWith(atKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /** rememberMe=false면 RT 미발급(null 반환). rememberMe=true면 단일 슬롯 저장 */
    public String createRefreshToken(Long employeeId, boolean rememberMe) {
        if (!rememberMe) return null; // ★ 핵심

        Date now = new Date();
        int days = props.getRefreshTokenExpiryDaysPersistent(); // 자동 로그인 전용 만료
        long rtMillis = Duration.ofDays(days).toMillis();

        Claims claims = Jwts.claims().setSubject(String.valueOf(employeeId));

        String rt = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + rtMillis))
                .signWith(rtKey, SignatureAlgorithm.HS512)
                .compact();

        // 계정 당 단일 슬롯
        redis.opsForValue().set(rtKey(employeeId), rt, Duration.ofMillis(rtMillis));
        return rt;
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(atKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** RT 서명/만료 검증 + Redis 저장 RT 일치 확인 */
    public Claims validateRefreshToken(String refreshToken) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(rtKey)
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        Long employeeId = Long.valueOf(claims.getSubject());
        String saved = redis.opsForValue().get(rtKey(employeeId));
        if (saved == null || !saved.equals(refreshToken)) {
            throw new JwtException("유효하지 않은 토큰입니다.");
        }
        return claims;
    }

    /** 로그아웃(계정 단위) */
    public void revokeRefreshToken(Long employeeId) {
        redis.delete(rtKey(employeeId));
    }
}
