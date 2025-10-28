package com.careup.ordering.domain.order.service;

import com.careup.ordering.common.client.BranchClient;
import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.notification.NotificationService;
import com.careup.ordering.domain.notification.SseNotificationResDto;
import com.careup.ordering.domain.order.dto.*;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderedItem;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.order.repository.OrderedItemRepository;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.InventoryFlowDetail;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import com.careup.ordering.domain.product.repository.InventoryFlowDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderedItemRepository orderedItemRepository;
    private final MemberRepository memberRepository;
    private final BranchProductRepository branchProductRepository;
    private final InventoryFlowDetailRepository inventoryFlowDetailRepository;
    private final NotificationService notificationService;
    /**
     * 주문 생성
     */
    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto requestDto) {
        log.info("주문 생성 시작 - memberId: {}, branchId: {}",
                requestDto.getMemberId(), requestDto.getBranchId());

        // 1. 회원 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + requestDto.getMemberId()));

        // 2. 총 금액 계산
        Long totalAmount = 0L;
        for (OrderItemRequestDto itemDto : requestDto.getOrderItems()) {
            BranchProduct branchProduct = branchProductRepository.findById(itemDto.getBranchProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 지점 상품입니다. ID: " + itemDto.getBranchProductId()));
            
            totalAmount += branchProduct.getPrice() * itemDto.getQuantity();
        }

        // 3. 주문 생성
        Order order = Order.builder()
                .member(member)
                .branchId(requestDto.getBranchId())
                .totalAmount(totalAmount)
                .orderType(requestDto.getOrderType())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("주문 생성 완료 - orderId: {}", savedOrder.getId());

        // 4. 주문 상품 생성 (✅ 커스텀 Builder - totalPrice 자동 계산)
        List<OrderedItem> orderedItems = requestDto.getOrderItems().stream()
                .map(itemDto -> {
                    BranchProduct branchProduct = branchProductRepository.findById(itemDto.getBranchProductId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "존재하지 않는 지점 상품입니다. ID: " + itemDto.getBranchProductId()));

                    // 재고 확인
                    if (branchProduct.getStockQuantity() < itemDto.getQuantity()) {
                        throw new IllegalStateException(
                                String.format("재고가 부족합니다. 상품: %s, 현재 재고: %d, 주문 수량: %d",
                                        branchProduct.getProduct().getName(),
                                        branchProduct.getStockQuantity(),
                                        itemDto.getQuantity()));
                    }

                    // 재고 감소
                    branchProduct.decreaseStock(itemDto.getQuantity());
                    log.info("재고 감소 - branchProductId: {}, 감소량: {}, 남은 재고: {}",
                            branchProduct.getId(), itemDto.getQuantity(), branchProduct.getStockQuantity());

                    // 재고 이력 생성
                    InventoryFlowDetail flowDetail = InventoryFlowDetail.builder()
                            .branchProduct(branchProduct)
                            .outQuantity(itemDto.getQuantity())
                            .remark("주문 ID: " + savedOrder.getId())
                            .build();
                    inventoryFlowDetailRepository.save(flowDetail);

                    // 안전 재고 체크
                    if (branchProduct.getStockQuantity() < branchProduct.getSafetystock()) {
                        log.warn("⚠️ 안전 재고 미만 - branchProductId: {}, 상품명: {}, 현재 재고: {}, 안전 재고: {}",
                                branchProduct.getId(),
                                branchProduct.getProduct().getName(),
                                branchProduct.getStockQuantity(),
                                branchProduct.getSafetystock());
                    }

                    // ✅ 커스텀 Builder 사용 (totalPrice는 자동 계산됨!)
                    return OrderedItem.builder()
                            .order(savedOrder)
                            .branchProduct(branchProduct)
                            .quantity(itemDto.getQuantity())
                            .unitPrice(branchProduct.getPrice())
                            .build();  // totalPrice는 생성자에서 자동 계산!
                })
                .collect(Collectors.toList());

        orderedItemRepository.saveAll(orderedItems);
        log.info("주문 상품 생성 완료 - 총 {}개", orderedItems.size());


        SseNotificationResDto dto = SseNotificationResDto.orderPlaced(savedOrder.getBranchId(), savedOrder.getId());
        notificationService.publishNotification(dto);

        return convertToResponseDto(savedOrder, orderedItems);
    }

    /**
     * 주문 상세 조회
     */
    public OrderResponseDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. ID: " + orderId));

        List<OrderedItem> orderedItems = orderedItemRepository.findByOrderId(orderId);

        return convertToResponseDto(order, orderedItems);
    }

    /**
     * 회원별 주문 목록 조회
     */
    public List<OrderResponseDto> getOrdersByMember(Long memberId) {
        List<Order> orders = orderRepository.findByMemberId(memberId);

        return orders.stream()
                .map(order -> {
                    List<OrderedItem> items = orderedItemRepository.findByOrderId(order.getId());
                    return convertToResponseDto(order, items);
                })
                .collect(Collectors.toList());
    }

    /**
     * 지점별 주문 목록 조회
     */
    public List<OrderResponseDto> getOrdersByBranch(Long branchId) {
        List<Order> orders = orderRepository.findByBranchId(branchId);

        return orders.stream()
                .map(order -> {
                    List<OrderedItem> items = orderedItemRepository.findByOrderId(order.getId());
                    return convertToResponseDto(order, items);
                })
                .collect(Collectors.toList());
    }

    /**
     * 주문 승인
     */
    @Transactional
    public OrderResponseDto approveOrder(Long orderId, Long approvedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. ID: " + orderId));

        order.approve(approvedBy);

        List<OrderedItem> items = orderedItemRepository.findByOrderId(orderId);
        SseNotificationResDto dto = SseNotificationResDto.orderApproved(order.getBranchId(), order.getId(), approvedBy);
        notificationService.publishNotification(dto);
        return convertToResponseDto(order, items);
    }

    /**
     * 주문 거부
     */
    @Transactional
    public OrderResponseDto rejectOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. ID: " + orderId));

        order.reject(reason);

        // 주문 거부 시 재고 복구
        List<OrderedItem> items = orderedItemRepository.findByOrderId(orderId);
        for (OrderedItem item : items) {
            BranchProduct branchProduct = item.getBranchProduct();
            branchProduct.increaseStock(item.getQuantity());
            
            InventoryFlowDetail flowDetail = InventoryFlowDetail.builder()
                    .branchProduct(branchProduct)
                    .inQuantity(item.getQuantity())
                    .remark("주문 거부 - 주문 ID: " + orderId)
                    .build();
            inventoryFlowDetailRepository.save(flowDetail);
            
            log.info("재고 복구 - branchProductId: {}, 복구량: {}", 
                    branchProduct.getId(), item.getQuantity());
        }

        SseNotificationResDto dto = SseNotificationResDto.orderRejected(order.getBranchId(), order.getId(), reason);
        notificationService.publishNotification(dto);

        return convertToResponseDto(order, items);
    }

    /**
     * 주문 취소
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. ID: " + orderId));

        order.cancel();

        // 주문 취소 시 재고 복구
        List<OrderedItem> items = orderedItemRepository.findByOrderId(orderId);
        for (OrderedItem item : items) {
            BranchProduct branchProduct = item.getBranchProduct();
            branchProduct.increaseStock(item.getQuantity());
            
            InventoryFlowDetail flowDetail = InventoryFlowDetail.builder()
                    .branchProduct(branchProduct)
                    .inQuantity(item.getQuantity())
                    .remark("주문 취소 - 주문 ID: " + orderId)
                    .build();
            inventoryFlowDetailRepository.save(flowDetail);

            SseNotificationResDto dto = SseNotificationResDto.orderCanceled(order.getBranchId(), order.getId());
            notificationService.publishNotification(dto);

            log.info("재고 복구 - branchProductId: {}, 복구량: {}", 
                    branchProduct.getId(), item.getQuantity());
        }
    }

    /**
     * Entity -> DTO 변환
     */
    private OrderResponseDto convertToResponseDto(Order order, List<OrderedItem> orderedItems) {
        List<OrderItemResponseDto> orderItemDtos = orderedItems.stream()
                .map(item -> OrderItemResponseDto.builder()
                        .orderItemId(item.getId())
                        .orderId(order.getId())
                        .branchProductId(item.getBranchProduct().getId())
                        .productId(item.getBranchProduct().getProduct().getId())
                        .productName(item.getBranchProduct().getProduct().getName())
                        .branchId(item.getBranchProduct().getBranchId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponseDto.builder()
                .orderId(order.getId())
                .memberId(order.getMember().getId())
                .memberName(order.getMember().getName())
                .branchId(order.getBranchId())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .orderType(order.getOrderType())
                .approvedBy(order.getApprovedBy())
                .approvedAt(order.getApprovedAt())
                .rejectedReason(order.getRejectedReason())
                .createdAt(order.getCreatedAt())
                .orderItems(orderItemDtos)
                .build();
    }
}
