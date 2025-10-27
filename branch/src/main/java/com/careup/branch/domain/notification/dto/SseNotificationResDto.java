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
    private Long branchId; // 알림 대상
    private String email;
    private String eventName;
    private String title;         // 알림 제목
    private String body;          // 알림 내용
    private String type;          // ex: STOCK_LOW, ORDER_PLACED
    private String action;

    public static SseNotificationResDto fromEntity(Notification notification){
        return SseNotificationResDto.builder()
                .email(notification.getMemberEmail())
                .eventName(notification.getEventName())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .action(notification.getAction())
                .build();

    }

    /** ✅ 출근 알림 */
    public static SseNotificationResDto attendanceCheckIn(String receiver, String employeeName) {
        return SseNotificationResDto.builder()
                .email(receiver)
                .eventName("ATTENDANCE")
                .title("👋 출근 체크 완료")
                .body(String.format("%s 님이 출근했습니다.", employeeName))
                .action("CHECK_IN")
                .build();
    }

    /** ✅ 퇴근 알림 */
    public static SseNotificationResDto attendanceCheckOut(String receiver, String employeeName) {
        return SseNotificationResDto.builder()
                .email(receiver)
                .eventName("ATTENDANCE")
                .title("🏠 퇴근 체크 완료")
                .body(String.format("%s 님이 퇴근했습니다.", employeeName))
                .action("CHECK_OUT")
                .build();
    }// ex: CHECK_IN, CANCEL, UPDATE 등

    /** ✅ 발주 상태 변경 알림 */
    public static SseNotificationResDto orderStatusChanged(String receiver, Long orderId, String status) {
        String title;
        String body;
        String eventName = "PURCHASE";
        String action = status.toUpperCase();

        switch (status.toUpperCase()) {
            case "REQUESTED" -> {
                title = "📦 발주 요청 접수";
                body = String.format("주문번호 [%d]의 발주 요청이 등록되었습니다.", orderId);
            }
            case "APPROVED" -> {
                title = "✅ 발주 승인 완료";
                body = String.format("주문번호 [%d]의 발주가 승인되었습니다.", orderId);
            }
            case "REJECTED" -> {
                title = "❌ 발주 반려 처리";
                body = String.format("주문번호 [%d]의 발주가 반려되었습니다.", orderId);
            }
            case "PARTIALLY_APPROVED" -> {
                title = "🟡 발주 부분 승인";
                body = String.format("주문번호 [%d]의 발주가 일부 승인되었습니다.", orderId);
            }
            default -> {
                title = "ℹ️ 발주 상태 변경";
                body = String.format("주문번호 [%d]의 상태가 [%s]로 변경되었습니다.", orderId, status);
            }
        }

        return SseNotificationResDto.builder()
                .email(receiver)
                .eventName(eventName)
                .title(title)
                .body(body)
                .type("ORDER")
                .action(action)
                .build();
    }
}
