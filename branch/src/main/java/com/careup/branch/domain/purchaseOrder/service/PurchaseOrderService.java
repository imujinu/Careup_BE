package com.careup.branch.domain.purchaseOrder.service;

import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderListResponseDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.careup.branch.domain.purchaseOrder.entity.OrderStatus;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrder;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrderDetail;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderRepository;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PurchaseOrderService {

    private static final Long HEAD_OFFICE_BRANCH_ID = 1L;
    private static final String PURCHASE_ORDER_APPROVED_TOPIC = "purchase-order-approved";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final KafkaTemplate<String, Object> purchaseOrderKafkaTemplate;
    
    // 발주 생성 (가맹점용)
    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto requestDto) {
        // 1. 검증
        validatePurchaseOrderRequest(requestDto);
        
        // 2. 발주 생성 (PENDING 상태로 시작)
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .branchId(requestDto.getBranchId())
                .orderStatus(OrderStatus.PENDING)
                .price(calculateTotalPrice(requestDto.getOrderDetails()))
                .build();
        
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        
        // 3. 발주 상세 내역 생성
        List<PurchaseOrderDetail> orderDetails = requestDto.getOrderDetails().stream()
                .filter(detail -> detail.getQuantity() > 0)
                .map(detailDto -> PurchaseOrderDetail.builder()
                        .purchaseOrder(savedOrder)
                        .productId(detailDto.getProductId())
                        .quantity(detailDto.getQuantity())
                        .approvedQuantity(0)
                        .unitPrice(detailDto.getUnitPrice())
                        .subtotalPrice(detailDto.getQuantity() * detailDto.getUnitPrice())
                        .build())
                .collect(Collectors.toList());
        
        // 4. 상세 내역 저장
        List<PurchaseOrderDetail> savedDetails = purchaseOrderDetailRepository.saveAll(orderDetails);

        return convertToResponseDto(savedOrder, savedDetails);
    }


    // 발주 요청 유효성 검증
    private void validatePurchaseOrderRequest(PurchaseOrderRequestDto requestDto) {
        if (requestDto.getBranchId() == null) {
            throw new IllegalArgumentException("가맹점 ID는 필수입니다.");
        }
        
        if (requestDto.getOrderDetails() == null || requestDto.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("발주 상품을 선택해주세요.");
        }
        
        // 수량이 0보다 큰 상품이 있는지 확인
        boolean hasValidQuantity = requestDto.getOrderDetails().stream()
                .anyMatch(detail -> detail.getQuantity() > 0);
        
        if (!hasValidQuantity) {
            throw new IllegalArgumentException("수량을 입력해주세요.");
        }
        
        // 단가가 0보다 큰지 확인
        boolean hasValidPrice = requestDto.getOrderDetails().stream()
                .allMatch(detail -> detail.getUnitPrice() > 0);
        
        if (!hasValidPrice) {
            throw new IllegalArgumentException("유효하지 않은 단가입니다.");
        }
    }

     // 총 금액 계산
    private long calculateTotalPrice(List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> orderDetails) {
        return orderDetails.stream()
                .filter(detail -> detail.getQuantity() > 0)
                .mapToLong(detail -> detail.getQuantity() * detail.getUnitPrice())
                .sum();
    }


    // 발주 목록 조회
    public List<PurchaseOrderListResponseDto> getPurchaseOrders(Long branchId) {
        if (branchId.equals(HEAD_OFFICE_BRANCH_ID)) {
            return getAllPurchaseOrders(); // 본사용 - 모든 발주 조회
        } else {
            return getPurchaseOrdersByBranch(branchId); // 가맹점용 - 특정 지점 발주 조회
        }
    }

    // 발주 목록 조회 (가맹점용)
    public List<PurchaseOrderListResponseDto> getPurchaseOrdersByBranch(Long branchId) {
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByBranchId(branchId);
        
        return purchaseOrders.stream()
                .map(this::convertToListResponseDto)
                .collect(Collectors.toList());
    }

    // 발주 목록 조회 (본사용)
    public List<PurchaseOrderListResponseDto> getAllPurchaseOrders() {
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll();

        return purchaseOrders.stream()
                .map(this::convertToListResponseDto)
                .collect(Collectors.toList());
    }

//    발주 상세 조회
    public PurchaseOrderResponseDto getPurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));

        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(purchaseOrder);

        return convertToResponseDto(purchaseOrder, orderDetails);
    }

    // 발주 승인 (본사용)
    @Transactional
    public PurchaseOrderResponseDto approvePurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));

        if (purchaseOrder.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 발주만 승인할 수 있습니다.");
        }
        
        purchaseOrder.changeOrderStatus(OrderStatus.APPROVED);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(savedOrder);
        
        // kafka로 발주 승인 이벤트 발송
        publishPurchaseOrderApprovedEvent(savedOrder, orderDetails);
        
        return convertToResponseDto(savedOrder, orderDetails);
    }

    // 발주 반려 (본사용)
    @Transactional
    public PurchaseOrderResponseDto rejectPurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));

        if (purchaseOrder.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 발주만 반려할 수 있습니다.");
        }
        
        purchaseOrder.changeOrderStatus(OrderStatus.REJECTED);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(savedOrder);
        return convertToResponseDto(savedOrder, orderDetails);
    }

    // 발주 부분 승인 (본사용)
    @Transactional
    public PurchaseOrderResponseDto partialApprovePurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));

        if (purchaseOrder.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 발주만 부분 승인할 수 있습니다.");
        }
        
        purchaseOrder.changeOrderStatus(OrderStatus.PARTIAL);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(savedOrder);
        return convertToResponseDto(savedOrder, orderDetails);
    }

    private PurchaseOrderListResponseDto convertToListResponseDto(PurchaseOrder purchaseOrder) {
        return PurchaseOrderListResponseDto.builder()
                .purchaseOrderId(purchaseOrder.getId())
                .branchId(purchaseOrder.getBranchId())
                .orderStatus(purchaseOrder.getOrderStatus())
                .totalPrice(purchaseOrder.getPrice())
                .createdAt(purchaseOrder.getCreatedAt())
                .updatedAt(purchaseOrder.getUpdatedAt())
                .build();
    }

    private PurchaseOrderResponseDto convertToResponseDto(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        return PurchaseOrderResponseDto.builder()
                .purchaseOrderId(purchaseOrder.getId())
                .branchId(purchaseOrder.getBranchId())
                .orderStatus(purchaseOrder.getOrderStatus())
                .totalPrice(purchaseOrder.getPrice())
                .createdAt(purchaseOrder.getCreatedAt())
                .updatedAt(purchaseOrder.getUpdatedAt())
                .orderDetails(orderDetails.stream()
                        .map(this::convertToDetailResponseDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto convertToDetailResponseDto(PurchaseOrderDetail detail) {
        return PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.builder()
                .purchaseOrderDetailId(detail.getId())
                .productId(detail.getProductId())
                .quantity(detail.getQuantity())
                .approvedQuantity(detail.getApprovedQuantity())
                .unitPrice(detail.getUnitPrice())
                .subtotalPrice(detail.getSubtotalPrice())
                .build();
    }
    
    // kafka 이벤트 발송
    // 발주 승인 이벤트 발송
    private void publishPurchaseOrderApprovedEvent(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        try {
            PurchaseOrderResponseDto event = convertToResponseDto(purchaseOrder, orderDetails);
            
            purchaseOrderKafkaTemplate.send(PURCHASE_ORDER_APPROVED_TOPIC, event);
            log.info("발주 승인 이벤트 발송 완료: purchaseOrderId={}, branchId={}, totalPrice={}", 
                purchaseOrder.getId(), purchaseOrder.getBranchId(), purchaseOrder.getPrice());
                
        } catch (Exception e) {
            log.error("발주 승인 이벤트 발송 실패: purchaseOrderId={}", purchaseOrder.getId(), e);
        }
    }
}