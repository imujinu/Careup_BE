package com.careup.branch.domain.notification.service;

import com.careup.branch.domain.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationAsyncWriter {
    private final NotificationService notificationService;

    @Async
    @Transactional
    public void saveAsync(Notification n) {
        notificationService.saveNotification(n);
    }
}
