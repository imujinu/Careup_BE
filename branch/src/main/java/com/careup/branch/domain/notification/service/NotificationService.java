package com.careup.branch.domain.notification.service;


import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.notification.dto.SseNotificationResDto;
import com.careup.branch.domain.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;
    public void publish(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }


    // [알림 목록 불러오기]
    public List<SseNotificationResDto> getNotificationList() {
        String email = null;
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        List<NotificationListResDto> notificationListResDtoList
//                = notificationRepository.findAllByUserAndState(user, NotificationState.UNREAD).stream().map(a -> NotificationListResDto.fromEntity(a)).toList();

        List<SseNotificationResDto> notificationListResDtoList
                = notificationRepository.findByMemberEmailAndIsReadFalse(email).stream().map(SseNotificationResDto::fromEntity).toList();

        log.info("[CAREUP][INFO] - NotificationService/getNotificationList - 알림목록 조회 성공");

        return notificationListResDtoList;
    }

    // [알림 읽음 처리]
    public void readNotification(Long notificationId) {

        notificationRepository.findById(notificationId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 알림 입니다.")).readNotification();

        log.info("[CAREUP][INFO] - NotificationService/getNotificationList - 알림 읽음 처리 성공");
    }

    public String getName(){
        return employeeRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 직원입니다.")).getName();
    }



}
