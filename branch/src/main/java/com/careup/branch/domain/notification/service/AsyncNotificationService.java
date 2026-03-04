package com.careup.branch.domain.notification.service;

import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.notification.domain.Notification;
import com.careup.branch.domain.notification.dto.SseNotificationResDto;
import com.careup.branch.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncNotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterRegistry sseEmitterRegistry;
    @Async // 비동기로 실행될 스레드 풀 지정
    public void sendAndSaveNotifications(List<DispatchStatus> dispatchStatus, SseNotificationResDto dto) {
        List<Notification> notificationBatch = new ArrayList<>();

        for (DispatchStatus ds : dispatchStatus) {
            String email = ds.getEmployee().getEmail();
            notificationBatch.add(new Notification().toEntity(dto, email));

            SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(email);
            if (sseEmitter != null) {
                try {
                    sseEmitter.send(SseEmitter.event().name(dto.getEventName()).data(dto));
                } catch (IOException e) {
                    sseEmitterRegistry.removeSseEmitter(email);
                }
            }
        }

        if (!notificationBatch.isEmpty()) {
            notificationRepository.saveAll(notificationBatch);
        }
        log.info("✔️ [비동기 워커] 125명 알림 처리 완료");
    }
}
