package com.careup.branch.common.auth;

/// JWT를 만들고 파싱(검증)할 때 필요
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/// 스프링 컴포넌트와 초기화 훅
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/// 시크릿(Base64) → 바이트 변환, 만료시간 계산에 사용
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {
    /// yml에서 읽어온 설정값 묶음(앞에서 만든 JwtProperties)
    private final JwtProperties props;

    /// RT를 저장/검증하기 위한 Redis. (opsForValue().get/set 사용)
    private final RedisTemplate<String, String> redisTemplate;

    /// 서명에 실제로 쓰이는 Key 객체(AT/RT 각각 따로)
    private Key atKey;
    private Key rtKey;

    /// 생성자: 스프링이 JwtProperties/RedisTemplate을 자동 주입
    public JwtTokenProvider(JwtProperties props,
                            @Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate) {
        this.props = props;
        this.redisTemplate = redisTemplate;
    }

    /// @PostConstruct: 스프링이 이 컴포넌트를 생성한 후 1회 호출
    /// 여기서 Base64 문자열을 "실제 키 바이트"로 변환
    @PostConstruct
    public void init() {
        /// *** Base64 → 바이트 → HS512용 HMAC 키로 변환 ***
        byte[] atBytes = Base64.getDecoder().decode(props.getSecretKeyAt());
        byte[] rtBytes = Base64.getDecoder().decode(props.getSecretKeyRt());

        /// HS512는 64바이트(=512비트) 이상 권장
        if (atBytes.length < 64) throw new IllegalArgumentException("HS512용 AT 키는 64바이트 이상이어야 합니다.");
        if (rtBytes.length < 64) throw new IllegalArgumentException("HS512용 RT 키는 64바이트 이상이어야 합니다.");

        /// 바이트 배열을 JWT 라이브러리가 이해하는 Key 객체로 변경
        this.atKey = Keys.hmacShaKeyFor(atBytes);
        this.rtKey = Keys.hmacShaKeyFor(rtBytes);
    }

    /// 로그인 성공 시, 짧게 들고 다닐 Access Token(AT)을 생성
    /// - subject: 로그인ID(예: loginId)
    /// - role/employeeId 같은 정보는 "클레임"으로 넣어 둠
    public String createAccessToken(String loginId, String role, Long employeeId) {
        /// 현재 시간(now)와 만료시간(ms)을 계산
        Date now = new Date();
        long atMillis = Duration.ofMinutes(props.getAccessTokenExpiryMinutes()).toMillis();

        /// 토큰 안에 들어갈 내용(payload)을 구성(클레임)
        Claims claims = Jwts.claims().setSubject(loginId);
        claims.put("role", role);
        if (employeeId != null) claims.put("employeeId", employeeId);

        /// HS512로 서명해서 최종 문자열(JWT)로 만들어 반환
        return Jwts.builder()
                .setClaims(claims)                                  /// 내용
                .setIssuedAt(now)                                   /// 발급시각
                .setExpiration(new Date(now.getTime() + atMillis))  /// 만료시각
                .signWith(atKey, SignatureAlgorithm.HS512)          /// 여기서 "서명(signature)" 발생
                .compact();
    }

    /// 자동로그인(rememberMe)에 따라 만료일이 다른 Refresh Token을 만들어 저장(서버 기억)
    /// - rememberMe가 true면 오래(예: 180일), false면 짧게(예: 1일)
    public String createRefreshToken(String loginId, boolean rememberMe) {
        Date now = new Date();
        int days = rememberMe ? props.getRefreshTokenExpiryDaysPersistent()
                : props.getRefreshTokenExpiryDaysNonPersistent();
        long rtMillis = Duration.ofDays(days).toMillis();

        /// RT는 보통 간단한 정보만 담고, 서버(Redis)에 저장해 재발급 시 대조
        Claims claims = Jwts.claims().setSubject(loginId);

        String rt = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + rtMillis))
                .signWith(rtKey, SignatureAlgorithm.HS512) // RT도 HS512로 서명
                .compact();

        /// Redis에 "RT:{loginId}" 라는 키로 저장 (유효기간과 동일하게 TTL 지정)
        redisTemplate.opsForValue().set("RT:" + loginId, rt, Duration.ofMillis(rtMillis));
        return rt;
    }

    /// 클라이언트가 보낸 AT가 "정상적으로 서명되고, 아직 안 만료됐는지" 검증하고
    /// 유효하면 안의 내용(클레임)을 꺼내 반환
    public Claims parseAccessToken(String token) {
        /// 서명이 틀리거나 만료되면 예외 처리 (401)
        return Jwts.parserBuilder()
                .setSigningKey(atKey)      /// AT 서명 키로 검증
                .build()
                .parseClaimsJws(token)     /// 검증+파싱
                .getBody();                /// payload(클레임) 반환
    }

    /// RT가 유효한지 검증한 후, 서버(Redis)에 저장된 것과 "정확히 같은지" 확인
    public Claims validateRefreshToken(String refreshToken) {
        /// 1) 서명/만료 검증
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(rtKey)      // RT 서명 키로 검증
                .build()
                .parseClaimsJws(refreshToken)
                .getBody();

        /// 2) 서버 저장값과 일치하는지 확인(탈취/로그아웃 등 통제 가능)
        String loginId = claims.getSubject();
        String saved = redisTemplate.opsForValue().get("RT:" + loginId);
        if (saved == null || !saved.equals(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 token 입니다."); /// 401로 처리될 에러
        }
        return claims;
    }
}
