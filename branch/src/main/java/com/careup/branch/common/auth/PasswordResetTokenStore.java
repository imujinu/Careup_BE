package com.careup.branch.common.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

@Component
public class PasswordResetTokenStore {

    private static final Duration TTL = Duration.ofMinutes(15);
    private final RedisTemplate<String, String> redis;

    public PasswordResetTokenStore(@Qualifier("rtInventory") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    private static String key(String email) { return "BRANCH:PWRESET:" + email; }

    public String issue(String email) {
        String token = generateToken();
        redis.opsForValue().set(key(email), token, TTL);
        return token;
    }

    public Optional<String> get(String email) {
        return Optional.ofNullable(redis.opsForValue().get(key(email)));
    }

    public void delete(String email) { redis.delete(key(email)); }

    private static String generateToken() {
        byte[] buf = new byte[18];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
