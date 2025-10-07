package com.careup.ordering.domain.order.service;

import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.order.dto.*;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderedItem;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.order.repository.OrderedItemRepository;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.repository.ProductRepository;
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
    private final ProductRepository productRepository;

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
        Long totalAmount = requestDto.getOrderItems().stream()
                .mapToLong(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        // 3. 주문 생성
        Order order = Order.builder()
                .member(member)
                .branchId(requestDto.getBranchId())
                .totalAmount(totalAmount)
                .orderType(requestDto.getOrderType())
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("주문 생성 완료 - orderId: {}", savedOrder.getId());

        // 4. 주문 상품 생성
        List<OrderedItem> orderedItems = requestDto.getOrderItems().stream()
                .map(itemDto -> {
                    Product product = productRepository.findById(itemDto.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "존재하지 않는 상품입니다. ID: " + itemDto.getProductId()));

                    return OrderedItem.builder()
                            .order(savedOrder)
                            .product(product)
                            .quantity(itemDto.getQuantity())
                            .unitPrice(itemDto.getUnitPrice())
                            .build();
                })
                .collect(Collectors.toList());

        orderedItemRepository.saveAll(orderedItems);
        log.info("주문 상품 생성 완료 - 총 {}개", orderedItems.size());

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

        List<OrderedItem> items = orderedItemRepository.findByOrderId(orderId);

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
    }

//    TODO:  convertToResponseDto 분리할것.
    private OrderResponseDto convertToResponseDto(Order order, List<OrderedItem> orderedItems) {
        List<OrderItemResponseDto> orderItemDtos = orderedItems.stream()
                .map(item -> OrderItemResponseDto.builder()
                        .orderItemId(item.getId())
                        .orderId(order.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
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