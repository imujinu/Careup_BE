package com.careup.ordering.domain.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SseNotificationResDto {
    private Long branchId;
    private String eventName;
    private String title;
    private String body;
    private String action;
    /** ✅ 주문 요청 알림 */
    public static SseNotificationResDto orderPlaced(Long branchId, Long orderId) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .eventName("ORDER")
                .title("🛒 신규 주문 발생")
                .body(String.format("주문 번호 %s 가 접수되었습니다.", orderId))
                .action("ORDER_PLACED")
                .build();
    }
    /** ✅ 주문 승인 알림 */
    public static SseNotificationResDto orderApproved(Long branchId, Long orderId) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .eventName("ORDER")
                .title("✅ 주문 승인 완료")
                .body(String.format("%s 님이 주문 번호 %d 을(를) 승인했습니다.", orderId))
                .action("ORDER_APPROVED")
                .build();
    }

    /** ❌ 주문 거부 알림 */
    public static SseNotificationResDto orderRejected(Long branchId, Long orderId, String reason) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .eventName("ORDER")
                .title("❌ 주문 거부")
                .body(String.format("%s 님이 주문 번호 %d 을(를) 거부했습니다.\n사유: %s", orderId, reason))
                .action("ORDER_REJECTED")
                .build();
    }

    /** 🔄 주문 취소 알림 */
    public static SseNotificationResDto orderCanceled(Long branchId, Long orderId) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .eventName("ORDER")
                .title("🔄 주문 취소")
                .body(String.format("%s 님이 주문 번호 %d 을(를) 취소했습니다.", orderId))
                .action("ORDER_CANCELED")
                .build();
    }

    /** 📦 재고 차감 알림 */
    public static SseNotificationResDto stockDecreased(Long branchId, String productName, long quantity) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .eventName("STOCK")
                .title("📉 재고 차감")
                .body(String.format("%s 상품의 재고가 %d개 차감되었습니다.", productName, quantity))
                .action("STOCK_DECREASE")
                .build();
    }

    /** 🔼 재고 증가 알림 */
    public static SseNotificationResDto stockIncreased(Long branchId, String productName, long quantity) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .eventName("STOCK")
                .title("📦 재고 추가")
                .body(String.format("%s 상품의 재고가 %d개 추가되었습니다.", productName, quantity))
                .action("STOCK_INCREASE")
                .build();
    }

    /** ⚙️ 재고 변경 알림 */
    public static SseNotificationResDto stockUpdated(Long branchId, String productName, long newQuantity) {
        return SseNotificationResDto.builder()
                .branchId(branchId)
                .eventName("STOCK")
                .title("🔁 재고 변경")
                .body(String.format("%s 상품의 재고가 %d개로 변경되었습니다.", productName, newQuantity))
                .action("STOCK_UPDATE")
                .build();
    }
}
