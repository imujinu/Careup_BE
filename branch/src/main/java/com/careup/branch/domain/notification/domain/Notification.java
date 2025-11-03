package com.careup.branch.domain.notification.domain;

import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.chat.entity.MemberType;
import com.careup.branch.domain.notification.dto.SseNotificationResDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String body;
    private String type;
    private String action;

    @Builder.Default
    private Boolean isRead = false;

    @Builder.Default
    private Boolean isDeleted = false;

    private Long branchId;
    private String senderName;
    private String receiverEmail;
    private String eventName;

    public void readNotification(){
        this.isRead = true;
    }

    public Notification toEntity(SseNotificationResDto dto, String email){
        return Notification.builder()
                .receiverEmail(email)
                .senderName(dto.getSenderName())
                .eventName(dto.getEventName())
                .title(dto.getTitle())
                .body(dto.getBody())
                .type(dto.getType())
                .action(dto.getAction())
                .branchId(dto.getBranchId())
                .build();
    }


}
