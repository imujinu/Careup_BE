package com.careup.branch.domain.notification.service;


import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.notification.domain.Notification;
import com.careup.branch.domain.notification.dto.SseNotificationResDto;
import com.careup.branch.domain.notification.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;

    public void saveNotification(Notification notification){
        notificationRepository.save(notification);
    }
    // [알림 목록 불러오기]
    public List<SseNotificationResDto> getNotificationList() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        String email = details.get("email").toString();

        System.out.println("emai: " + email);
        List<SseNotificationResDto> notificationListResDtoList
                = notificationRepository.findByReceiverEmailAndIsReadFalse(email).stream().map(SseNotificationResDto::fromEntity).toList();

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

    // [알림 전체 읽음 처리]
    public void readAllNotification(List<Long> notificationIds) {

        for(Long id : notificationIds){

        notificationRepository.findById(id).orElseThrow(()->new EntityNotFoundException("존재하지 않는 알림 입니다.")).readNotification();
        }


        log.info("[CAREUP][INFO] - NotificationService/getNotificationList - 전체 알림 읽음 처리 성공");
    }

    /**
     * 특정 이메일 리스트에게 알림 발송 및 저장
     */
    public void sendNotificationToEmails(List<String> receiverEmails, SseNotificationResDto notificationDto) {
        for (String email : receiverEmails) {
            Notification notification = Notification.builder()
                    .receiverEmail(email)
                    .senderName(notificationDto.getSenderName())
                    .eventName(notificationDto.getEventName())
                    .title(notificationDto.getTitle())
                    .body(notificationDto.getBody())
                    .type(notificationDto.getType())
                    .action(notificationDto.getAction())
                    .branchId(notificationDto.getBranchId())
                    .isRead(false)
                    .isDeleted(false)
                    .build();

            notificationRepository.save(notification);
            log.info("[CAREUP][INFO] - NotificationService/sendNotificationToEmails - 알림 저장 완료: {}", email);
        }
    }

    /**
     * 단일 이메일에게 알림 발송 및 저장
     */
    public void sendNotificationToEmail(String receiverEmail, SseNotificationResDto notificationDto) {
        Notification notification = Notification.builder()
                .receiverEmail(receiverEmail)
                .senderName(notificationDto.getSenderName())
                .eventName(notificationDto.getEventName())
                .title(notificationDto.getTitle())
                .body(notificationDto.getBody())
                .type(notificationDto.getType())
                .action(notificationDto.getAction())
                .branchId(notificationDto.getBranchId())
                .isRead(false)
                .isDeleted(false)
                .build();

        notificationRepository.save(notification);
        log.info("[CAREUP][INFO] - NotificationService/sendNotificationToEmail - 알림 저장 완료: {}", receiverEmail);
    }

}
