package com.careup.ordering.domain.order.service;

import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.order.dto.OrderRequestDto;
import com.careup.ordering.domain.order.dto.OrderResponseDto;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.order.repository.OrderedItemRepository;
import com.careup.ordering.domain.payment.repository.PaymentRepository;
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
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;


    @Transactional
    public OrderRequestDto createOrder(OrderRequestDto requestDto){

        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(()-> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Long totalAmount = requestDto.getOrderItems().stream()
                .mapToLong(item-> item.getUnitPrice() * item.getQuantity())
                .sum();

        Order order = Order.builder()
                .memberId(requestDto.getMemberId())
                .branchId(requestDto.getBranchId())
                .orderType(requestDto.getOrderType())
                .orderStau
    }
}
