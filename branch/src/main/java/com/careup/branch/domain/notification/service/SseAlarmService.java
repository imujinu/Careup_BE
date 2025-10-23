package com.careup.branch.domain.notification.service;

import com.careup.branch.domain.notification.dto.SseNotificationResDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseAlarmService {
    private final SseEmitterRegistry sseEmitterRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishNotification(String receiver, String eventName) {
        SseNotificationResDto dto = SseNotificationResDto.builder()
                .receiver(receiver)
                .eventName(eventName)
                .build();

        String data;
        try {
            data = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // 항상 Kafka로 발행
        kafkaTemplate.send("notification-topic", data);
    }

    // Kafka 수신 -> emitter 있는 클라이언트에게 전송
    @KafkaListener(topics = "notification-topic", groupId = "sse-group")
    public void onMessage(String message) {
        try {
            SseNotificationResDto dto = objectMapper.readValue(message, SseNotificationResDto.class);

            SseEmitter sseEmitter = sseEmitterRegistry.getEmitter(dto.getReceiver());
            if (sseEmitter != null) {
                try {
                    sseEmitter.send(SseEmitter.event().name(dto.getEventName()).data(dto));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


