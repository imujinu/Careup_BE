package com.careup.ordering.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedissonClient redissonClient;

    /*
     * @param lockKey
     * @param waitTime 락 획득 대기 시간
     * @param leaseTime 락 보유 시간
     * @param task
     * @return
     */

    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, LockTask<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            log.info("락 획득 시도: {}", lockKey);
            
            // 락 획득 시도
            boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            
            if (acquired) {
                log.info("락 획득 성공: {}", lockKey);
                try {
                    return task.execute();
                } finally {
                    // 락 해제
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.info("락 해제 완료: {}", lockKey);
                    }
                }
            } else {
                log.warn("락 획득 실패: {} (대기시간 초과)", lockKey);
                throw new RuntimeException("재고 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            throw new RuntimeException("락 처리 중 오류가 발생했습니다.", e);
        }
    }

   // 재고 관리용 분산 락
    public <T> T executeInventoryLock(Long branchProductId, LockTask<T> task) {
        String lockKey = "inventory:lock:" + branchProductId;
        return executeWithLock(lockKey, 5, 10, task); // 5초 대기, 10초 보유
    }

    // 락 작업 인터페이스
    @FunctionalInterface
    public interface LockTask<T> {
        T execute() throws Exception;
    }
}
