package com.careup.branch.domain.purchaseOrder.service;

import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.purchaseOrder.dto.PartialApproveRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderListResponseDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.careup.branch.domain.purchaseOrder.entity.OrderStatus;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrder;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrderDetail;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderRepository;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderDetailRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PurchaseOrderService {

    private static final Long HEAD_OFFICE_BRANCH_ID = 1L;
    private static final String PURCHASE_ORDER_APPROVED_TOPIC = "purchase-order-approved";
    private static final String PURCHASE_ORDER_COMPLETED_TOPIC = "purchase-order-completed";

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final KafkaTemplate<String, Object> purchaseOrderKafkaTemplate;
    private final OrderingInventoryClient orderingInventoryClient;
    private final EmployeeRepository employeeRepository;
    
    // 발주 생성 (가맹점용)
    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto requestDto) {
        // 1. 검증
        validatePurchaseOrderRequest(requestDto);
        
        // 2. 상품별 공급가 조회 및 설정
        List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> details =
                setSupplyPrices(requestDto.getOrderDetails());
        
        // 3. 발주 생성 (PENDING 상태로 시작)
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .branchId(requestDto.getBranchId())
                .orderStatus(OrderStatus.PENDING)
                .price(calculateTotalPrice(details))
                .build();
        
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        
        // 4. 발주 상세 내역 생성
        List<PurchaseOrderDetail> orderDetails = details.stream()
                .filter(detail -> detail.getQuantity() > 0)
                .map(detailDto -> PurchaseOrderDetail.builder()
                        .purchaseOrder(savedOrder)
                        .productId(detailDto.getProductId())
                        .quantity(detailDto.getQuantity())
                        .approvedQuantity(0)
                        .unitPrice(detailDto.getSupplyPrice())
                        .subtotalPrice(detailDto.getQuantity() * detailDto.getSupplyPrice())
                        .build())
                .collect(Collectors.toList());
        
        // 5. 상세 내역 저장
        List<PurchaseOrderDetail> savedDetails = purchaseOrderDetailRepository.saveAll(orderDetails);


        return convertToResponseDto(savedOrder, savedDetails);
    }
    
    // 공급가 조회 및 설정
    private List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> setSupplyPrices(
            List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> orderDetails) {
        
        return orderDetails.stream()
                .map(detail -> {
                    // ordering 서버에서 상품 정보 조회
                    OrderingInventoryClient.ProductResponseDto product = 
                        orderingInventoryClient.getProduct(detail.getProductId());
                    
                    // 공급가 자동 설정
                    detail.setSupplyPrice(product.supplyPrice);

                    
                    return detail;
                })
                .collect(Collectors.toList());
    }
    
    // 검증된 데이터로 총액 계산
    private long calculateTotalPrice(
            List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> orderDetails) {
        return orderDetails.stream()
                .filter(detail -> detail.getQuantity() > 0)
                .mapToLong(detail -> detail.getQuantity() * detail.getSupplyPrice())
                .sum();
    }


    // 발주 요청 유효성 검증
    private void validatePurchaseOrderRequest(PurchaseOrderRequestDto requestDto) {
        if (requestDto.getBranchId() == null) {
            throw new IllegalArgumentException("가맹점 ID는 필수입니다.");
        }
        
        // 지점 권한 검증
        validateBranchAccess(requestDto.getBranchId());
        
        if (requestDto.getOrderDetails() == null || requestDto.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("발주 상품을 선택해주세요.");
        }
        
        // 수량이 0보다 큰 상품이 있는지 확인
        boolean hasValidQuantity = requestDto.getOrderDetails().stream()
                .anyMatch(detail -> detail.getQuantity() > 0);
        
        if (!hasValidQuantity) {
            throw new IllegalArgumentException("수량을 입력해주세요.");
        }
    }

    /**
     * 지점 접근 권한 검증
     * - 본사 관리자: 모든 지점 접근 가능
     * - 가맹점주/직원: 자신의 지점만 접근 가능
     */
    private void validateBranchAccess(Long branchId) {
        try {
            // 1. 현재 인증된 사용자 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new SecurityException("인증되지 않은 사용자입니다.");
            }

            // 2. JWT Claims에서 사용자 정보 추출
            Claims claims = (Claims) auth.getDetails();
            if (claims == null) {
                throw new SecurityException("JWT 토큰 정보를 찾을 수 없습니다.");
            }
            
            Long employeeId = claims.get("employeeId", Long.class);
            String role = claims.get("role", String.class);

            // 3. 본사 관리자는 모든 지점 접근 가능
            if ("HQ_ADMIN".equals(role)) {
                log.info("본사 관리자 권한으로 모든 지점 접근 허용");
                return;
            }

            // 4. 가맹점주/직원인 경우 자신의 지점만 접근 가능
            Employee employee = employeeRepository.findWithDispatchStatusesById(employeeId)
                    .orElseThrow(() -> new SecurityException("사용자 정보를 찾을 수 없습니다."));

            // 5. 현재 활성화된 지점 배치 확인
            List<Long> accessibleBranchIds = getAccessibleBranchIds(employee);
            
            if (!accessibleBranchIds.contains(branchId)) {
                log.warn("지점 접근 권한 없음 - employeeId: {}, accessibleBranches: {}, requestedBranch: {}", 
                        employeeId, accessibleBranchIds, branchId);
                throw new SecurityException("해당 지점에 대한 접근 권한이 없습니다.");
            }


        } catch (Exception e) {
            throw new SecurityException("지점 접근 권한 검증에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자가 접근 가능한 지점 ID 목록 조회
     */
    private List<Long> getAccessibleBranchIds(Employee employee) {
        LocalDate now = LocalDate.now();
        
        return employee.getDispatchStatuses().stream()
                .filter(dispatch -> dispatch.getAssignedFrom().isBefore(now) || dispatch.getAssignedFrom().isEqual(now))
                .filter(dispatch -> dispatch.getAssignedTo().isAfter(now) || dispatch.getAssignedTo().isEqual(now))
                .map(dispatch -> dispatch.getBranch().getId())
                .distinct()
                .toList();
    }



    // 발주 목록 조회
    public List<PurchaseOrderListResponseDto> getPurchaseOrders(Long branchId) {
        // 지점 접근 권한 검증
        validateBranchAccess(branchId);
        
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
        
        // 발주가 속한 지점에 대한 접근 권한 검증
        validateBranchAccess(purchaseOrder.getBranchId());

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
        
        // 전체 승인 시 approvedQuantity를 요청 수량으로 설정
        for (PurchaseOrderDetail detail : orderDetails) {
            detail.setApprovedQuantity(detail.getQuantity());
        }
        orderDetails = purchaseOrderDetailRepository.saveAll(orderDetails);
        
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
    public PurchaseOrderResponseDto partialApprovePurchaseOrder(Long purchaseOrderId, PartialApproveRequestDto requestDto) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));

        if (purchaseOrder.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 발주만 부분 승인할 수 있습니다.");
        }
        
        // 발주 상세 내역 조회
        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(purchaseOrder);
        
        // 승인할 수량 설정
        setApprovedQuantities(orderDetails, requestDto.getApprovedDetails());
        
        // 승인 상태 결정
        determineApprovalStatus(purchaseOrder, orderDetails);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        // 상세 내역 저장
        List<PurchaseOrderDetail> savedDetails = purchaseOrderDetailRepository.saveAll(orderDetails);
        
        // kafka 이벤트 발송 (승인된 수량만)
        publishPartialApprovedEvent(savedOrder, savedDetails);


        return convertToResponseDto(savedOrder, savedDetails);
    }

    /**
     * - APPROVED: 모든 상품이 요청 수량대로 승인
     * - REJECTED: 모든 상품이 승인 거부
     * - PARTIAL: 일부는 승인, 일부는 부분 승인 또는 거부
     */
    private void determineApprovalStatus(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        // 모든 상품이 전체 승인되었는지 확인
        boolean allFullyApproved = orderDetails.stream()
                .allMatch(detail -> detail.getApprovedQuantity() == detail.getQuantity());

        // 모든 상품이 거부되었는지 확인
        boolean allRejected = orderDetails.stream()
                .allMatch(detail -> detail.getApprovedQuantity() == 0);

        if (allFullyApproved) {
            purchaseOrder.changeOrderStatus(OrderStatus.APPROVED);
            log.info("발주 전체 승인: purchaseOrderId={}", purchaseOrder.getId());
        } else if (allRejected) {
            purchaseOrder.changeOrderStatus(OrderStatus.REJECTED);
            log.info("발주 전체 거부: purchaseOrderId={}", purchaseOrder.getId());
        } else {
            purchaseOrder.changeOrderStatus(OrderStatus.PARTIAL);
            log.info("발주 부분 승인: purchaseOrderId={}", purchaseOrder.getId());
        }
    }
    
    // 승인할 수량 설정
    private void setApprovedQuantities(List<PurchaseOrderDetail> orderDetails, 
                                     List<PartialApproveRequestDto.PartialApproveDetailDto> approvedDetails) {
        
        for (PurchaseOrderDetail orderDetail : orderDetails) {
            // 승인 요청에서 해당 상품 찾기
            PartialApproveRequestDto.PartialApproveDetailDto approvedDetail = approvedDetails.stream()
                    .filter(detail -> detail.getProductId().equals(orderDetail.getProductId()))
                    .findFirst()
                    .orElse(null);
            
            if (approvedDetail != null) {
                // 승인 수량 검증
                if (approvedDetail.getApprovedQuantity() > orderDetail.getQuantity()) {
                    throw new IllegalArgumentException(
                        String.format("승인 수량(%d)이 요청 수량(%d)을 초과할 수 없습니다.", 
                            approvedDetail.getApprovedQuantity(), orderDetail.getQuantity()));
                }
                
                if (approvedDetail.getApprovedQuantity() <= 0) {
                    throw new IllegalArgumentException("승인 수량은 1개 이상이어야 합니다.");
                }
                
                // 승인 수량 설정
                orderDetail.setApprovedQuantity(approvedDetail.getApprovedQuantity());
                
                log.info("상품 부분 승인: productId={}, 요청수량={}, 승인수량={}", 
                    orderDetail.getProductId(), orderDetail.getQuantity(), approvedDetail.getApprovedQuantity());
            } else {
                // 승인 요청에 없는 상품은 0개 승인
                orderDetail.setApprovedQuantity(0);
                log.info("상품 승인 제외: productId={}, 요청수량={}, 승인수량=0", 
                    orderDetail.getProductId(), orderDetail.getQuantity());
            }
        }
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
    
    // 발주 부분 승인 이벤트 발송 (승인된 수량만 재고 차감)
    private void publishPartialApprovedEvent(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        try {
            // 승인된 수량만 포함한 이벤트 생성
            PurchaseOrderResponseDto event = convertToPartialApprovedResponseDto(purchaseOrder, orderDetails);
            
            purchaseOrderKafkaTemplate.send(PURCHASE_ORDER_APPROVED_TOPIC, event);
                
        } catch (Exception e) {
            log.error("발주 부분 승인 이벤트 발송 실패: purchaseOrderId={}", purchaseOrder.getId(), e);
        }
    }
    
    // 부분 승인된 상품만 포함한 응답 dto
    private PurchaseOrderResponseDto convertToPartialApprovedResponseDto(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        // 승인된 수량이 0보다 큰 상품에 대하여
        List<PurchaseOrderDetail> approvedDetails = orderDetails.stream()
                .filter(detail -> detail.getApprovedQuantity() > 0)
                .collect(Collectors.toList());
        
        // 승인된 수량으로 새로운 상세 내역 생성
        List<PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto> approvedDetailDtos = approvedDetails.stream()
                .map(detail -> PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.builder()
                        .purchaseOrderDetailId(detail.getId())
                        .productId(detail.getProductId())
                        .quantity(detail.getApprovedQuantity()) // 승인된 수량 사용
                        .approvedQuantity(detail.getApprovedQuantity())
                        .unitPrice(detail.getUnitPrice())
                        .subtotalPrice(detail.getApprovedQuantity() * detail.getUnitPrice())
                        .build())
                .collect(Collectors.toList());
        
        return PurchaseOrderResponseDto.builder()
                .purchaseOrderId(purchaseOrder.getId())
                .branchId(purchaseOrder.getBranchId())
                .orderStatus(purchaseOrder.getOrderStatus())
                .totalPrice(calculatePartialApprovedTotalPrice(approvedDetails))
                .orderDetails(approvedDetailDtos)
                .createdAt(purchaseOrder.getCreatedAt())
                .updatedAt(purchaseOrder.getUpdatedAt())
                .build();
    }
    
    // 부분 승인된 총액 계산
    private long calculatePartialApprovedTotalPrice(List<PurchaseOrderDetail> approvedDetails) {
        return approvedDetails.stream()
                .mapToLong(detail -> detail.getApprovedQuantity() * detail.getUnitPrice())
                .sum();
    }

    // 발주 배송 시작 (본사용)
    @Transactional
    public PurchaseOrderResponseDto shipPurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));

        if (purchaseOrder.getOrderStatus() != OrderStatus.APPROVED && 
            purchaseOrder.getOrderStatus() != OrderStatus.PARTIAL) {
            throw new IllegalStateException("승인된 발주만 배송 시작할 수 있습니다. 현재 상태: " + purchaseOrder.getOrderStatus());
        }
        
        // 상태를 SHIPPED로 변경
        purchaseOrder.changeOrderStatus(OrderStatus.SHIPPED);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(savedOrder);

        return convertToResponseDto(savedOrder, orderDetails);
    }

    // 발주 입고 완료 (가맹점용)
    @Transactional
    public PurchaseOrderResponseDto completePurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));

        if (purchaseOrder.getOrderStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("배송 중인 발주만 입고 완료할 수 있습니다. 현재 상태: " + purchaseOrder.getOrderStatus());
        }
        
        // 상태를 COMPLETED로 변경
        purchaseOrder.changeOrderStatus(OrderStatus.COMPLETED);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(savedOrder);
        
        // 가맹점 재고 증가 이벤트 발송
        publishInventoryIncreaseEvent(savedOrder, orderDetails);

        return convertToResponseDto(savedOrder, orderDetails);
    }

     // 가맹점 재고 증가 이벤트 발송
    private void publishInventoryIncreaseEvent(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        try {
            // 승인된 수량만 포함한 이벤트 생성 (재고 증가용)
            PurchaseOrderResponseDto event = convertToCompletedResponseDto(purchaseOrder, orderDetails);
            
            purchaseOrderKafkaTemplate.send(PURCHASE_ORDER_COMPLETED_TOPIC, event);
                
        } catch (Exception e) {
            log.error("발주 입고 완료 이벤트 발송 실패: purchaseOrderId={}", purchaseOrder.getId(), e);
        }
    }

    /**
     * 입고 완료용 응답 dto (승인된 수량만 포함)
     */
    private PurchaseOrderResponseDto convertToCompletedResponseDto(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        List<PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto> detailDtos = orderDetails.stream()
                .filter(detail -> detail.getApprovedQuantity() > 0)
                .map(detail -> PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.builder()
                        .purchaseOrderDetailId(detail.getId())
                        .productId(detail.getProductId())
                        .quantity(detail.getApprovedQuantity()) // 승인된 수량을 재고 증가 수량으로 사용
                        .approvedQuantity(detail.getApprovedQuantity())
                        .unitPrice(detail.getUnitPrice())
                        .subtotalPrice(detail.getApprovedQuantity() * detail.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return PurchaseOrderResponseDto.builder()
                .purchaseOrderId(purchaseOrder.getId())
                .branchId(purchaseOrder.getBranchId())
                .orderStatus(purchaseOrder.getOrderStatus())
                .totalPrice(calculatePartialApprovedTotalPrice(orderDetails))
                .orderDetails(detailDtos)
                .createdAt(purchaseOrder.getCreatedAt())
                .updatedAt(purchaseOrder.getUpdatedAt())
                .build();
    }
}