package com.careup.ordering.domain.auth.oauth.store;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthStateStore {

    private static final String KEY = "ORDERING:OAUTH:STATE:"; // + state
    private static final Duration TTL = Duration.ofMinutes(10);

    @Qualifier("rtInventory")
    private final RedisTemplate<String, String> redis;

    /** 서버가 발급하는 state(10분 TTL) */
    public String issue() {
        String state = randomToken();
        redis.opsForValue().set(KEY + state, "1", TTL);
        return state;
    }

    /** 일회성 소비: 있으면 삭제 후 true, 없으면 false */
    public boolean consume(String state) {
        if (state == null || state.isBlank()) return false;
        String key = KEY + state;
        Boolean exists = redis.hasKey(key);
        if (Boolean.TRUE.equals(exists)) {
            redis.delete(key);
            return true;
        }
        return false;
    }

    private static String randomToken() {
        byte[] buf = new byte[24];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
