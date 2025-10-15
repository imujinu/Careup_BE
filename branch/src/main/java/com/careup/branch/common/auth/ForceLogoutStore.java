package com.careup.branch.common.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class ForceLogoutStore {

    private static final String KEY = "BRANCH:REVOKE_BEFORE:"; // + employeeId
    private static final Duration TTL = Duration.ofDays(30);

    private final RedisTemplate<String, String> redis;

    public ForceLogoutStore(@Qualifier("rtInventory") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public void scheduleCutoverAfterSeconds(Long employeeId, int seconds) {
        long cutoverAtMillis = System.currentTimeMillis() + (seconds * 1000L);
        redis.opsForValue().set(KEY + employeeId, String.valueOf(cutoverAtMillis), TTL);
    }

    public Optional<Long> readCutoverAt(Long employeeId) {
        String v = redis.opsForValue().get(KEY + employeeId);
        if (v == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(v));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public boolean isTokenObsolete(Long employeeId, long tokenIssuedAtMillis) {
        Optional<Long> cutoverAt = readCutoverAt(employeeId);
        return cutoverAt.map(cut -> tokenIssuedAtMillis < cut).orElse(false);
    }

    public void clear(Long employeeId) {
        redis.delete(KEY + employeeId);
    }
}
