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

    private static String rtKey(Long memberId) { return "RT:CUS:" + memberId; }

    public String createAccessToken(Long memberId, String role) {
        Date now = new Date();
        long atMillis = Duration.ofMinutes(props.getAccessTokenExpiryMinutes()).toMillis();

        Claims claims = Jwts.claims().setSubject(String.valueOf(memberId));
        claims.put("role", role);
        claims.put("memberId", memberId);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + atMillis))
                .signWith(atKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String createRefreshToken(Long memberId, boolean rememberMe) {
        if (!rememberMe) return null;

        Date now = new Date();
        long rtMillis = Duration.ofDays(props.getRefreshTokenExpiryDaysPersistent()).toMillis();

        Claims claims = Jwts.claims().setSubject(String.valueOf(memberId));

        String rt = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + rtMillis))
                .signWith(rtKey, SignatureAlgorithm.HS512)
                .compact();

        redis.opsForValue().set(rtKey(memberId), rt, Duration.ofMillis(rtMillis));
        return rt;
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parserBuilder().setSigningKey(atKey).build()
                .parseClaimsJws(token).getBody();
    }

    public Claims validateRefreshToken(String refreshToken) {
        Claims claims = Jwts.parserBuilder().setSigningKey(rtKey).build()
                .parseClaimsJws(refreshToken).getBody();

        Long memberId = Long.valueOf(claims.getSubject());
        String saved = redis.opsForValue().get(rtKey(memberId));
        if (saved == null || !saved.equals(refreshToken)) {
            throw new JwtException("유효하지 않은 토큰입니다.");
        }
        return claims;
    }

    public void revokeRefreshToken(Long memberId) {
        redis.delete(rtKey(memberId));
    }
}
