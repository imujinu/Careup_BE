package com.careup.ordering.domain.auth.oauth.store;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OauthStateStore {

    private static final String KEY = "ORDERING:OAUTH:STATE:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> redis;

    public OauthStateStore(@Qualifier("rtInventory") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public String issue() {
        String state = randomToken();
        redis.opsForValue().set(KEY + state, "1", TTL);
        return state;
    }

    public boolean consume(String state) {
        if (state == null || state.isBlank()) return false;
        String key = KEY + state;
        String val = redis.opsForValue().getAndDelete(key); // Redis 6.2+ / Spring Data Redis 2.6+
        return val != null;
    }

    private static String randomToken() {
        byte[] buf = new byte[24];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
