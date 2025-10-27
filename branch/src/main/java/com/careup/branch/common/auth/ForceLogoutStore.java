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

    /** now + seconds 기준으로 컷오프 시각 저장 */
    public void scheduleCutoverAfterSeconds(Long employeeId, int seconds) {
        long cutoverAtMillis = System.currentTimeMillis() + (seconds * 1000L);
        redis.opsForValue().set(KEY + employeeId, String.valueOf(cutoverAtMillis), TTL);
    }

    /** 저장된 컷오프 시각(ms) 조회 */
    public Optional<Long> readCutoverAt(Long employeeId) {
        String v = redis.opsForValue().get(KEY + employeeId);
        if (v == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(v));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * 토큰 무효 판정:
     *  - 현재시각이 cutoverAt 에 도달(또는 경과)했고
     *  - 토큰 발급시각(iat)이 cutoverAt 보다 이전이면 무효
     */
    public boolean isTokenObsolete(Long employeeId, long tokenIssuedAtMillis) {
        return readCutoverAt(employeeId)
                .map(cut -> System.currentTimeMillis() >= cut && tokenIssuedAtMillis < cut)
                .orElse(false);
    }

    /** 특정 사용자 컷오프 클리어 */
    public void clear(Long employeeId) {
        redis.delete(KEY + employeeId);
    }
}
