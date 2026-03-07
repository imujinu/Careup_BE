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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
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
    private final NotificationRepository notificationRepository;
    private final NotificationAsyncWriter asyncWriter;
    private final AsyncNotificationService asyncNotificationService;
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
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
        try {

            SseNotificationResDto dto = objectMapper.readValue(message, SseNotificationResDto.class);
            Branch branch = branchRepository.findById(dto.getBranchId()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 지점입니다."));
            List<DispatchStatus> dispatchStatus = dispatchStatusRepository.findAllByBranch(branch);

            long totalStartTime = System.currentTimeMillis();
            String threadName = Thread.currentThread().getName();

            if ("ATTENDANCE".equals(dto.getEventName())){
                Employee owner = getOwner(dto.getBranchId());
                SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(owner.getEmail());
                if (sseEmitter != null) {
                    try {
                        sseEmitter.send(SseEmitter.event().name(dto.getEventName()).data(dto));
                        Notification notification = new Notification().toEntity(dto,owner.getEmail());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }else{

                log.info("[알림 루프 시작] 대상 인원: {}, 스레드: {}",
                        dispatchStatus.size(), threadName);
//                List<Notification> notificationBatch = new ArrayList<>();
//
//                for (DispatchStatus ds : dispatchStatus) {
//                    String email = ds.getEmployee().getEmail();
//
//                    // 2. 루프 안에서는 리스트에 담기만 함 (DB 통신 발생 X)
//                    notificationBatch.add(new Notification().toEntity(dto, email));
//
//                    // 3. SSE 전송은 실시간성이 중요하므로 루프 내에서 그대로 수행
//                    SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(email);
//                    if (sseEmitter != null) {
//                        try {
//                            sseEmitter.send(SseEmitter.event().name(dto.getEventName()).data(dto));
//                        } catch (IOException e) {
//                            log.info("SSE 연결 만료: {}", email);
//                            sseEmitter.complete();
//                            sseEmitterRegistry.removeSseEmitter(email);
//                        }
//                    }
//                }
//
//                // 4. [핵심] 루프가 끝난 후 한 번에 저장 (125번의 호출 -> 1번의 호출)
//                if (!notificationBatch.isEmpty()) {
//                    notificationRepository.saveAll(notificationBatch);
//                }
//
            }
            asyncNotificationService.sendAndSaveNotifications(dispatchStatus, dto);
                long totalDuration = System.currentTimeMillis() - totalStartTime;
                log.info("[알림 루프 종료]  총 소요시간: {}ms, 스레드: {}",
                         totalDuration, threadName);

        } catch (IOException e) {
            log.error("❌ Kafka listener error: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }finally {
            stopWatch.stop();
            // stopWatch.getTotalTimeSeconds()를 쓰면 바로 초 단위가 나옵니다.
            log.info("[알림 루프 종료] 총 소요시간: {}s", stopWatch.getTotalTimeSeconds());
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


