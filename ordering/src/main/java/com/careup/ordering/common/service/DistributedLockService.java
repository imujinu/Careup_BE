package com.careup.ordering.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
            throw new RuntimeException("재고 처리 중입니다. 잠시 후 다시 시도해주세요.", e);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // 재고 부족 등의 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            // 원래 예외 메시지를 포함하여 전파
            String errorMessage = e.getMessage() != null && !e.getMessage().isEmpty() 
                    ? e.getMessage() 
                    : "재고 처리 중 오류가 발생했습니다.";
            throw new RuntimeException(errorMessage, e);
        }
    }

   // 재고 관리용 분산 락
    public <T> T executeInventoryLock(Long branchProductId, LockTask<T> task) {
        String lockKey = "inventory:lock:" + branchProductId;
        return executeWithLock(lockKey, 5, 10, task); // 5초 대기, 10초 보유
    }

    // 트랜잭션 커밋 이후에 락을 해제
    public <T> T executeInventoryLockUntilCommit(Long branchProductId, LockTask<T> task) {
        String lockKey = "inventory:lock:" + branchProductId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            log.info("락 획득 시도: {}", lockKey);
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("락 획득 실패: {} (대기시간 초과)", lockKey);
                throw new RuntimeException("재고 처리 중입니다. 잠시 후 다시 시도해주세요.");
            }

            log.info("락 획득 성공: {}", lockKey);

            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            if (txActive) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        try {
                            if (lock.isHeldByCurrentThread()) {
                                lock.unlock();
                                log.info("락 해제 완료: {}", lockKey);
                            }
                        } catch (Exception e) {
                            log.warn("락 해제 실패: {} - {}", lockKey, e.getMessage());
                        }
                    }
                });
                return task.execute();
            } else {
                try {
                    return task.execute();
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.info("락 해제 완료(비트랜잭션): {}", lockKey);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("재고 처리 중입니다. 잠시 후 다시 시도해주세요.", e);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // 재고 부족 등의 예외는 그대로 전파
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("락 해제 완료(예외): {}", lockKey);
                }
            } catch (Exception ignore) {}
            throw e;
        } catch (Exception e) {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.info("락 해제 완료(예외): {}", lockKey);
                }
            } catch (Exception ignore) {}
            // 원래 예외 메시지를 포함하여 전파
            String errorMessage = e.getMessage() != null && !e.getMessage().isEmpty() 
                    ? e.getMessage() 
                    : "재고 처리 중 오류가 발생했습니다.";
            throw new RuntimeException(errorMessage, e);
        }
    }

    // 락 작업 인터페이스
    @FunctionalInterface
    public interface LockTask<T> {
        T execute() throws Exception;
    }
}
