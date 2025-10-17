package com.careup.ordering.domain.auth.oauth.store;

import com.careup.ordering.domain.member.entity.SocialProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthTempStore {

    private static final String KEY = "ORDERING:OAUTH:PENDING:"; // + token
    private static final Duration TTL = Duration.ofMinutes(15);

    @Qualifier("rtInventory")
    private final RedisTemplate<String, String> redis;
    private final ObjectMapper om = new ObjectMapper();

    public String save(Payload p) {
        String token = randomToken();
        try {
            String json = om.writeValueAsString(p);
            redis.opsForValue().set(KEY + token, json, TTL);
            return token;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("임시 상태 저장 실패", e);
        }
    }

    public Optional<Payload> consume(String token) {
        String raw = redis.opsForValue().get(KEY + token);
        if (raw == null) return Optional.empty();
        redis.delete(KEY + token); // 일회성 사용
        try {
            return Optional.of(om.readValue(raw, Payload.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String randomToken() {
        byte[] buf = new byte[24];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    public static record Payload(
            SocialProvider provider,
            String socialId,
            String email,
            String name,
            String profileImageUrl
    ) {}
}
