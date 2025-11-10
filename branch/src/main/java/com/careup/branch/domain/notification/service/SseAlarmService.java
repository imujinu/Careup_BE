package com.careup.branch.domain.notification.service;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.chat.service.ChatUserService;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.notification.domain.Notification;
import com.careup.branch.domain.notification.dto.SseNotificationResDto;
import com.careup.branch.domain.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SseAlarmService {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final BranchRepository branchRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final NotificationService notificationService;
    public void publishNotification(SseNotificationResDto dto) {

        try {
            kafkaTemplate.send("notification-topic", objectMapper.writeValueAsString(dto));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // Kafka 수신 -> emitter 있는 클라이언트에게 전송
    @KafkaListener(topics = "notification-topic", groupId = "sse-group")
    public void onMessage(String message) {
        try {
            SseNotificationResDto dto = objectMapper.readValue(message, SseNotificationResDto.class);
            Branch branch = branchRepository.findById(dto.getBranchId()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 지점입니다."));
            List<DispatchStatus> dispatchStatus = dispatchStatusRepository.findAllByBranch(branch);
            if ("ATTENDANCE".equals(dto.getEventName())){
                Employee owner = getOwner(dto.getBranchId());
                SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(owner.getEmail());
                Notification notification = new Notification().toEntity(dto, owner.getEmail());
                notificationService.saveNotification(notification);
                if (sseEmitter != null) {
                    try {
                        sseEmitter.send(SseEmitter.event().name(dto.getEventName()).data(dto));

                    } catch (IOException e) {
                        log.info("SSE 연결이 닫혔습니다: {}", e.getMessage());
                        sseEmitter.complete(); // ✅ 정상 종료로 처리
                        sseEmitterRegistry.removeSseEmitter(owner.getEmail());
                    }
                }

            }else{
                for(DispatchStatus ds : dispatchStatus){
            SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(ds.getEmployee().getEmail());
            Notification notification = new Notification().toEntity(dto, ds.getEmployee().getEmail());
            notificationService.saveNotification(notification);
            if (sseEmitter != null) {
                try {
                    sseEmitter.send(SseEmitter.event().name(dto.getEventName()).data(dto));

                } catch (IOException e) {
                    log.info("SSE 연결이 닫혔습니다: {}", e.getMessage());
                    sseEmitter.complete(); // ✅ 정상 종료로 처리
                    sseEmitterRegistry.removeSseEmitter(ds.getEmployee().getEmail());
                }
            }
                }
            }

        } catch (IOException e) {
            log.error("❌ Kafka listener error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Employee getOwner(Long branchId){

        List<DispatchStatus> list = dispatchStatusRepository.findAllByBranchId(branchId);

        //  HQ_ADMIN,           /// 본사(본점) 관리자
        //    BRANCH_ADMIN,       /// 지점(직영) 관리자
        //    FRANCHISE_OWNER,
        return list.stream()
                .filter(em -> em.getEmployee().getAuthorityType().equals(AuthorityType.HQ_ADMIN)
                        || em.getEmployee().getAuthorityType().equals(AuthorityType.BRANCH_ADMIN)
                        || em.getEmployee().getAuthorityType().equals(AuthorityType.FRANCHISE_OWNER))
                .findFirst().orElseThrow(()-> new EntityNotFoundException("관리자가 존재하지 않습니다.")).getEmployee();

    }
}


