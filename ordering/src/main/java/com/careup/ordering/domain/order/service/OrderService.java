package com.careup.ordering.domain.order.service;

import com.careup.ordering.common.service.DistributedLockService;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.member.service.LoyalCustomerService;
import com.careup.ordering.domain.notification.NotificationService;
import com.careup.ordering.domain.notification.SseNotificationResDto;
import com.careup.ordering.domain.order.dto.*;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderedItem;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.order.repository.OrderedItemRepository;
import com.careup.ordering.domain.payment.entity.Payment;
import com.careup.ordering.domain.payment.entity.PaymentStatus;
import com.careup.ordering.domain.payment.repository.PaymentRepository;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.InventoryFlowDetail;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import com.careup.ordering.domain.product.repository.InventoryFlowDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    @Autowired(required = false)
    private NotificationService notificationService;

    private final LoyalCustomerService loyalCustomerService;
    private final DistributedLockService distributedLockService;
    private final PaymentRepository paymentRepository;

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

        // 2. 재고 확인 및 총 금액 계산 (분산 락 사용하여 동시성 제어)
        // 여러 상품이 있을 때 deadlock 방지를 위해 branchProductId를 정렬하여 처리
        List<OrderItemRequestDto> sortedItems = new ArrayList<>(requestDto.getOrderItems());
        sortedItems.sort((a, b) -> Long.compare(a.getBranchProductId(), b.getBranchProductId()));

        // 재고 확인 및 총 금액 계산을 위한 임시 데이터 저장
        List<BranchProduct> reservedProducts = new ArrayList<>();
        List<Long> reservedQuantities = new ArrayList<>();
        List<Long> reservedPrices = new ArrayList<>();

        // 먼저 모든 재고를 확인하고 감소 (락 안에서)
        try {
            for (OrderItemRequestDto itemDto : sortedItems) {
                // 각 상품별로 분산 락을 사용하여 재고 확인 및 감소
                distributedLockService.executeInventoryLockUntilCommit(
                        itemDto.getBranchProductId(),
                        () -> {
                            // 락 안에서 최신 재고 확인
                            BranchProduct branchProduct = branchProductRepository.findById(itemDto.getBranchProductId())
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "존재하지 않는 지점 상품입니다. ID: " + itemDto.getBranchProductId()));

                            // 재고 확인 (락 안에서 최신 재고 확인)
                            if (branchProduct.getStockQuantity() < itemDto.getQuantity()) {
                                String productName = branchProduct.getProduct().getName();
                                Long currentStock = branchProduct.getStockQuantity();
                                Long requestedQuantity = itemDto.getQuantity();

                                String errorMessage;
                                if (currentStock == 0) {
                                    errorMessage = String.format("죄송합니다. '%s' 상품의 재고가 모두 소진되었습니다. 다른 상품을 선택해주세요.", productName);
                                } else {
                                    errorMessage = String.format("죄송합니다. '%s' 상품의 재고가 부족합니다. (현재 재고: %d개, 주문 수량: %d개)\n\n재고가 있는 수량으로 다시 주문해주세요.",
                                            productName, currentStock, requestedQuantity);
                                }

                                throw new IllegalStateException(errorMessage);
                            }

                            // 재고 감소
                            branchProduct.decreaseStock(itemDto.getQuantity());
                            log.info("재고 감소 - branchProductId: {}, 감소량: {}, 남은 재고: {}",
                                    branchProduct.getId(), itemDto.getQuantity(), branchProduct.getStockQuantity());

                            // 안전 재고 체크
                            if (branchProduct.getStockQuantity() < branchProduct.getSafetystock()) {
                                log.warn("안전 재고 미만 - branchProductId: {}, 상품명: {}, 현재 재고: {}, 안전 재고: {}",
                                        branchProduct.getId(),
                                        branchProduct.getProduct().getName(),
                                        branchProduct.getStockQuantity(),
                                        branchProduct.getSafetystock());
                            }

                            // 재고가 확보된 상품 정보 저장
                            reservedProducts.add(branchProduct);
                            reservedQuantities.add(itemDto.getQuantity());
                            reservedPrices.add(branchProduct.getPrice());

                            return null;
                        }
                );
            }
        } catch (IllegalStateException e) {
            // 재고 부족 시 예외를 그대로 전파 (트랜잭션 롤백되어 재고 복구됨)
            log.error("재고 부족으로 주문 생성 실패: {}", e.getMessage());
            throw e;
        }

        // 총 금액 계산
        Long totalAmount = 0L;
        for (int i = 0; i < reservedProducts.size(); i++) {
            totalAmount += reservedPrices.get(i) * reservedQuantities.get(i);
        }

        // 3. 모든 재고 확인이 완료된 후 주문 생성
        Order order = Order.builder()
                .member(member)
                .branchId(requestDto.getBranchId())
                .totalAmount(totalAmount)
                .orderType(requestDto.getOrderType())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("주문 생성 완료 - orderId: {}", savedOrder.getId());

        // 4. 주문 상품 생성 및 재고 이력 생성
        List<OrderedItem> orderedItems = new ArrayList<>();
        for (int i = 0; i < reservedProducts.size(); i++) {
            BranchProduct branchProduct = reservedProducts.get(i);
            Long quantity = reservedQuantities.get(i);
            Long unitPrice = reservedPrices.get(i);

            // OrderedItem 생성
            OrderedItem orderedItem = OrderedItem.builder()
                    .order(savedOrder)
                    .branchProduct(branchProduct)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();
            orderedItems.add(orderedItem);

            // 재고 이력 생성
            InventoryFlowDetail flowDetail = InventoryFlowDetail.builder()
                    .branchProduct(branchProduct)
                    .outQuantity(quantity)
                    .remark("주문 ID: " + savedOrder.getId())
                    .build();
            inventoryFlowDetailRepository.save(flowDetail);
        }

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
     * 전체 주문 목록 조회 (본사용)
     */
    public List<OrderResponseDto> getAllOrders() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream()
                .map(order -> {
                    List<OrderedItem> items = orderedItemRepository.findByOrderId(order.getId());
                    return convertToResponseDto(order, items);
                })
                .collect(Collectors.toList());
    }

    /**
     * 주문 승인 (결제 완료된 주문만 승인 가능)
     */
    @Transactional
    public OrderResponseDto approveOrder(Long orderId, Long approvedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. ID: " + orderId));

        // 결제 완료 여부 확인
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isEmpty() || paymentOpt.get().getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("결제가 완료되지 않은 주문은 승인할 수 없습니다. orderId: " + orderId);
        }

        order.approve(approvedBy);
        log.info("주문 승인 완료 - orderId: {}, approvedBy: {}, 결제 완료 확인됨", orderId, approvedBy);

        // 단골 고객 정보 자동 업데이트
        try {
            loyalCustomerService.updateLoyalCustomerByOrder(
                    order.getMember().getId(),
                    order.getBranchId(),
                    java.math.BigDecimal.valueOf(order.getTotalAmount())
            );
            log.info("단골 고객 정보 업데이트 완료 - memberId: {}, branchId: {}, amount: {}",
                    order.getMember().getId(), order.getBranchId(), order.getTotalAmount());
        } catch (Exception e) {
            log.error("단골 고객 정보 업데이트 실패: {}", e.getMessage());
            // 주문 승인은 계속 진행 (단골 고객 업데이트 실패가 주문을 막지 않음)
        }

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
        }

        SseNotificationResDto dto = SseNotificationResDto.orderCanceled(order.getBranchId(), order.getId());
        notificationService.publishNotification(dto);
    }

    /**
     * 타임아웃 주문 자동 취소 (결제 미완료 주문)
     */
    @Transactional
    public int cancelTimeoutOrders(int timeoutMinutes) {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<Order> timeoutOrders = orderRepository.findByOrderStatusAndCreatedAtBefore(
                com.careup.ordering.domain.order.entity.OrderStatus.PENDING, timeoutThreshold);

        int cancelledCount = 0;
        for (Order order : timeoutOrders) {
            // 이미 취소되었거나 결제가 완료된 주문은 건너뛰기
            if (order.getOrderStatus() != com.careup.ordering.domain.order.entity.OrderStatus.PENDING) {
                continue;
            }

            // 결제 여부 확인
            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(order.getId());

            // 결제가 완료되지 않은 주문만 취소
            if (paymentOpt.isEmpty() ||
                paymentOpt.get().getPaymentStatus() != PaymentStatus.COMPLETED) {

                log.info("타임아웃 주문 취소 시작 - orderId: {}, 생성시간: {}",
                        order.getId(), order.getCreatedAt());

                order.cancel();

                // 재고 복구
                List<OrderedItem> items = orderedItemRepository.findByOrderId(order.getId());
                for (OrderedItem item : items) {
                    BranchProduct branchProduct = item.getBranchProduct();
                    branchProduct.increaseStock(item.getQuantity());

                    InventoryFlowDetail flowDetail = InventoryFlowDetail.builder()
                            .branchProduct(branchProduct)
                            .inQuantity(item.getQuantity())
                            .remark("타임아웃 주문 취소 - 주문 ID: " + order.getId())
                            .build();
                    inventoryFlowDetailRepository.save(flowDetail);

                    log.info("타임아웃 주문 재고 복구 - branchProductId: {}, 복구량: {}",
                            branchProduct.getId(), item.getQuantity());
                }

                SseNotificationResDto dto = SseNotificationResDto.orderCanceled(order.getBranchId(), order.getId());
                notificationService.publishNotification(dto);

                cancelledCount++;
                log.info("타임아웃 주문 취소 완료 - orderId: {}", order.getId());
            }
        }

        if (cancelledCount > 0) {
            log.info("타임아웃 주문 취소 완료 - 총 {}개 주문 취소", cancelledCount);
        }

        return cancelledCount;
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

        // 결제 정보 조회
        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(order.getId());
        PaymentStatus paymentStatus = null;
        Boolean isPaymentCompleted = false;

        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            paymentStatus = payment.getPaymentStatus();
            isPaymentCompleted = payment.getPaymentStatus() == PaymentStatus.COMPLETED;
        }

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
                .paymentStatus(paymentStatus)
                .isPaymentCompleted(isPaymentCompleted)
                .build();
    }
}
