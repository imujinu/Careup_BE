package com.careup.ordering.common.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class CustomerRevokeStore {

    private static final String KEY = "ORDERING:REVOKE_BEFORE:"; // + memberId
    private static final Duration TTL = Duration.ofDays(30);

    private final RedisTemplate<String, String> redis;

    public CustomerRevokeStore(@Qualifier("rtInventory") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public void scheduleCutoverAfterSeconds(Long memberId, int seconds) {
        long cutoverAtMillis = System.currentTimeMillis() + (seconds * 1000L);
        redis.opsForValue().set(KEY + memberId, String.valueOf(cutoverAtMillis), TTL);
    }

    public Optional<Long> readCutoverAt(Long memberId) {
        String v = redis.opsForValue().get(KEY + memberId);
        if (v == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(v));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public boolean isTokenObsolete(Long memberId, long tokenIssuedAtMillis) {
        Optional<Long> cutoverAt = readCutoverAt(memberId);
        return cutoverAt.map(cut -> tokenIssuedAtMillis < cut).orElse(false);
    }

    public void clear(Long memberId) {
        redis.delete(KEY + memberId);
    }
}
