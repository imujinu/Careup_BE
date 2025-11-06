package com.careup.branch.domain.notification.dto;

import com.careup.branch.domain.notification.domain.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SseNotificationResDto {
    private Long id;
    private Long branchId; // 알림 대상
    private String memberEmail;
    private String senderName;
    private String eventName;
    private String title;         // 알림 제목
    private String body;          // 알림 내용
    private String type;          // ex: STOCK_LOW, ORDER_PLACED
    private String action;
    private boolean isRead;

    public static SseNotificationResDto fromEntity(Notification notification){
        return SseNotificationResDto.builder()
                .id(notification.getId())
                .branchId(notification.getBranchId())
                .eventName(notification.getEventName())
                .memberEmail(notification.getReceiverEmail())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .action(notification.getAction())
                .isRead(notification.getIsRead())
                .build();

    }

    /** ✅ 출근 알림 */
    public static SseNotificationResDto attendanceCheckIn(String employeeName, Long branchId) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .senderName(employeeName)
                .eventName("ATTENDANCE")
                .title("👋 출근 체크 완료")
                .body(String.format("%s 님이 출근했습니다.", employeeName))
                .action("CHECK_IN")
                .build();
    }

    /** ✅ 퇴근 알림 */
    public static SseNotificationResDto attendanceCheckOut(String employeeName, Long branchId) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .senderName(employeeName)
                .eventName("ATTENDANCE")
                .title("🏠 퇴근 체크 완료")
                .body(String.format("%s 님이 퇴근했습니다.", employeeName))
                .action("CHECK_OUT")
                .build();
    }// ex: CHECK_IN, CANCEL, UPDATE 등

    /** ✅ 발주 상태 변경 알림 */
    public static SseNotificationResDto orderStatusChanged(String senderBranch, Long orderId, String status, Long branchId) {
        String title;
        String body;
        String eventName = "PURCHASE";
        String action = status.toUpperCase();

        switch (status.toUpperCase()) {
            case "REQUESTED" -> {
                title = "📦 발주 요청 접수";
                body = String.format("발주번호 : [%d], 지점명 : [%s]의 발주 요청이 등록되었습니다.", orderId, senderBranch);
            }
            case "APPROVED" -> {
                title = "✅ 발주 승인 완료";
                body = String.format("발주번호 : [%d] 의 발주가 승인되었습니다.", orderId);
            }
            case "REJECTED" -> {
                title = "❌ 발주 반려 처리";
                body = String.format("발주번호 : [%d]의 발주가 반려되었습니다.", orderId);
            }
            case "PARTIALLY_APPROVED" -> {
                title = "🟡 발주 부분 승인";
                body = String.format("발주번호 :[%d]의 발주가 일부 승인되었습니다.", orderId);
            }
            default -> {
                title = "ℹ️ 발주 상태 변경";
                body = String.format("발주번호 : [%d], 지점명 : [%s]의 발주가 [%s]되었습니다.", orderId,senderBranch, status);
            }
        }

        return SseNotificationResDto.builder()
                .branchId(branchId)
                .senderName(senderBranch)
                .eventName(eventName)
                .title(title)
                .body(body)
                .type("ORDER")
                .action(action)
                .build();
    }

    /** ✅ 지점 정보 수정 요청 알림 (본사 관리자에게) */
    public static SseNotificationResDto branchUpdateRequested(String branchName, Long requestId, String requesterName) {
        return SseNotificationResDto.builder()
                .senderName(requesterName)
                .eventName("BRANCH_UPDATE")
                .title("🏢 지점 정보 수정 요청")
                .body(String.format("[%s] 지점의 정보 수정 요청이 접수되었습니다. 승인/거부 처리가 필요합니다.", branchName))
                .type("BRANCH_UPDATE_REQUEST")
                .action("REQUESTED")
                .build();
    }

    /** ✅ 지점 정보 수정 승인 알림 (요청자에게) */
    public static SseNotificationResDto branchUpdateApproved(String branchName, Long branchId, String approverName) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .senderName(approverName)
                .eventName("BRANCH_UPDATE")
                .title("✅ 지점 정보 수정 승인")
                .body(String.format("[%s] 지점의 정보 수정 요청이 승인되었습니다.", branchName))
                .type("BRANCH_UPDATE_REQUEST")
                .action("APPROVED")
                .build();
    }

    /** ✅ 지점 정보 수정 거부 알림 (요청자에게) */
    public static SseNotificationResDto branchUpdateRejected(String branchName, Long branchId, String approverName) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .senderName(approverName)
                .eventName("BRANCH_UPDATE")
                .title("❌ 지점 정보 수정 거부")
                .body(String.format("[%s] 지점의 정보 수정 요청이 거부되었습니다.", branchName))
                .type("BRANCH_UPDATE_REQUEST")
                .action("REJECTED")
                .build();
    }
}
