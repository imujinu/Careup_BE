package com.careup.branch.domain.purchaseOrder.service;

import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.domain.chat.service.ChatUserService;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.notification.dto.SseNotificationResDto;
import com.careup.branch.domain.notification.service.SseAlarmService;
import com.careup.branch.common.client.OrderingInventoryClient.ResponseDto;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.purchaseOrder.dto.PartialApproveRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderListResponseDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.careup.branch.domain.purchaseOrder.entity.OrderStatus;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrder;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrderDetail;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderDetailRepository;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
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

    private final DispatchStatusRepository dispatchStatusRepository;
    private final EmployeeRepository employeeRepository;
    private final SseAlarmService sseAlarmService;

    private final BranchRepository branchRepository;

    // 발주 생성 (가맹점용)
    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto requestDto) {
        // 1) 기본 검증
        validatePurchaseOrderRequest(requestDto);

        // 2) 중복 발주 방지
        validateNoDuplicateOrder(requestDto);

        // 3) 상품별 공급가 조회(주문서의 공급가를 서버에서 확정) + null 가드
        List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> details = setSupplyPrices(requestDto.getOrderDetails());

        // 4) 주문 저장
        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .branchId(requestDto.getBranchId())
                .orderStatus(OrderStatus.PENDING)
                .price(calculateTotalPrice(details)) // Long 언박싱 NPE 방지 로직 포함
                .build();

        // 5) 상세 정보 저장
        List<PurchaseOrderDetail> orderDetails = details.stream()
                .map(detail -> {
                    String productName = getProductName(detail.getProductId());
                    return PurchaseOrderDetail.builder()
                            .purchaseOrder(purchaseOrder)
                            .productId(detail.getProductId())
                            .productName(productName)
                            .quantity(detail.getQuantity())
                            .approvedQuantity(detail.getQuantity())
                            .unitPrice(detail.getSupplyPrice())
                            .subtotalPrice((long) detail.getQuantity() * detail.getSupplyPrice())
                            .build();
                })
                .toList();

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        
        // 상세 정보 저장
        orderDetails.forEach(detail -> detail.setPurchaseOrder(savedOrder));
        orderDetails.forEach(purchaseOrderDetailRepository::save);

        // 6) 본사 재고 예약 (요청 시점 소프트 예약) - 실패 시 롤백
        reserveHqStocksOrThrow(savedOrder, orderDetails);

        log.info("발주 생성 완료: {}", savedOrder.getId());
        return PurchaseOrderResponseDto.builder()
                .purchaseOrderId(savedOrder.getId())
                .branchId(savedOrder.getBranchId())
                .orderStatus(savedOrder.getOrderStatus())
                .totalPrice(savedOrder.getPrice())
                .createdAt(savedOrder.getCreatedAt())
                .build();
    }

    // 자동 발주 생성
    @Transactional
    public PurchaseOrderResponseDto createAutoPurchaseOrder(PurchaseOrderRequestDto requestDto) {
        try {
            // 1) 기본 검증
            validatePurchaseOrderRequestWithoutAuth(requestDto);

            // 2) 중복 발주 방지
            validateNoDuplicateOrder(requestDto);

            // 3) 상품별 공급가 조회
            List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> details = setSupplyPrices(requestDto.getOrderDetails());

            // 4) 주문 저장
            PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                    .branchId(requestDto.getBranchId())
                    .orderStatus(OrderStatus.PENDING)
                    .price(calculateTotalPrice(details))
                    .build();

            PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

            // 5) 상세 저장
            List<PurchaseOrderDetail> orderDetails = details.stream()
                    .filter(detail -> detail.getQuantity() > 0)
                    .map(detailDto -> {
                        // 상품명 조회
                        String productName = getProductName(detailDto.getProductId());
                        return PurchaseOrderDetail.builder()
                                .purchaseOrder(savedOrder)
                                .productId(detailDto.getProductId())
                                .productName(productName)
                                .quantity(detailDto.getQuantity())
                                .approvedQuantity(0)
                                .unitPrice(detailDto.getSupplyPrice())
                                .subtotalPrice(detailDto.getQuantity() * detailDto.getSupplyPrice())
                                .build();
                    })
                    .collect(Collectors.toList());

            List<PurchaseOrderDetail> savedDetails = purchaseOrderDetailRepository.saveAll(orderDetails);

//            //[알림 - 발주 요청]
//            List<DispatchStatus> employees = dispatchStatusRepository.findAllByBranchId(HEAD_OFFICE_BRANCH_ID);
//            for(DispatchStatus ds : employees){
//                SseNotificationResDto dto = SseNotificationResDto.orderStatusChanged(ds.getEmployee().getEmail(), purchaseOrder.getId(), "REQUESTED");
//                sseAlarmService.publishNotification(dto);
//            }

        // 본사 재고 예약 (자동 발주도 동일 정책)
        reserveHqStocksOrThrow(savedOrder, savedDetails);

            return convertToCompletedResponseDto(savedOrder, savedDetails);

        } catch (SecurityException e) {
            throw new RuntimeException("자동 발주 생성 실패: " + e.getMessage(), e);
        }
    }

    private void reserveHqStocksOrThrow(PurchaseOrder savedOrder, List<PurchaseOrderDetail> orderDetails) {
        Long hqBranchId = 1L;
        for (PurchaseOrderDetail detail : orderDetails) {
            try {
                OrderingInventoryClient.BranchProductResponseDto hqBp = orderingInventoryClient.getBranchProduct(hqBranchId, detail.getProductId());
                if (hqBp == null || hqBp.branchProductId == null) {
                    throw new IllegalStateException("본사 재고를 찾을 수 없습니다. productId=" + detail.getProductId());
                }
                OrderingInventoryClient.ReservationRequest req = new OrderingInventoryClient.ReservationRequest();
                req.branchProductId = hqBp.branchProductId;
                req.quantity = (long) detail.getQuantity();
                req.reason = "PURCHASE_ORDER_REQUEST";
                req.purchaseOrderId = savedOrder.getId();
                orderingInventoryClient.reserve(req);
            } catch (Exception e) {
                log.error("예약재고 확보 실패: orderId={}, productId={}, msg={}", savedOrder.getId(), detail.getProductId(), e.getMessage());
                throw new RuntimeException("예약재고 확보 실패: productId=" + detail.getProductId(), e);
            }
        }
    }

    private void releaseHqStocks(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        Long hqBranchId = 1L;
        for (PurchaseOrderDetail detail : orderDetails) {
            try {
                OrderingInventoryClient.BranchProductResponseDto hqBp = orderingInventoryClient.getBranchProduct(hqBranchId, detail.getProductId());
                if (hqBp == null || hqBp.branchProductId == null) {
                    log.warn("본사 재고를 찾을 수 없어 예약 해제 실패: productId={}", detail.getProductId());
                    continue;
                }
                OrderingInventoryClient.ReservationRequest req = new OrderingInventoryClient.ReservationRequest();
                req.branchProductId = hqBp.branchProductId;
                req.quantity = (long) detail.getQuantity();
                req.reason = "PURCHASE_ORDER_CANCELLED";
                req.purchaseOrderId = purchaseOrder.getId();
                orderingInventoryClient.releaseReservation(req);
                log.info("예약재고 해제 완료: orderId={}, productId={}, quantity={}", 
                    purchaseOrder.getId(), detail.getProductId(), detail.getQuantity());
            } catch (Exception e) {
                log.error("예약재고 해제 실패: orderId={}, productId={}, msg={}", 
                    purchaseOrder.getId(), detail.getProductId(), e.getMessage());
                // 예약 해제 실패는 경고만 하고 계속 진행 (발주 취소는 완료되어야 함)
            }
        }
    }

    private void releasePartialHqStocks(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        Long hqBranchId = 1L;
        for (PurchaseOrderDetail detail : orderDetails) {
            // 거부된 수량만큼만 예약 해제
            long rejectedQuantity = detail.getQuantity() - detail.getApprovedQuantity();
            if (rejectedQuantity <= 0) {
                continue;
            }
            
            try {
                OrderingInventoryClient.BranchProductResponseDto hqBp = orderingInventoryClient.getBranchProduct(hqBranchId, detail.getProductId());
                if (hqBp == null || hqBp.branchProductId == null) {
                    log.warn("본사 재고를 찾을 수 없어 예약 해제 실패: productId={}", detail.getProductId());
                    continue;
                }
                OrderingInventoryClient.ReservationRequest req = new OrderingInventoryClient.ReservationRequest();
                req.branchProductId = hqBp.branchProductId;
                req.quantity = rejectedQuantity;
                req.reason = "PURCHASE_ORDER_PARTIAL_REJECT";
                req.purchaseOrderId = purchaseOrder.getId();
                orderingInventoryClient.releaseReservation(req);
                log.info("부분 예약재고 해제 완료: orderId={}, productId={}, rejectedQuantity={}", 
                    purchaseOrder.getId(), detail.getProductId(), rejectedQuantity);
            } catch (Exception e) {
                log.error("부분 예약재고 해제 실패: orderId={}, productId={}, msg={}", 
                    purchaseOrder.getId(), detail.getProductId(), e.getMessage());
            }
        }
    }

    // 공급가 조회 및 설정 (Feign 응답 언랩 + null 가드)
    private List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> setSupplyPrices(
            List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> orderDetails) {

        return orderDetails.stream()
                .map(detail -> {
                    try {
                        log.info("상품 정보 조회 시도: productId={}", detail.getProductId());

                        ResponseDto<OrderingInventoryClient.ProductResponseDto> resp =
                                orderingInventoryClient.getProduct(detail.getProductId());

                        if (resp == null || resp.data == null || resp.data.supplyPrice == null) {
                            throw new IllegalArgumentException("상품 공급가 조회 실패: productId=" + detail.getProductId());
                        }

                        // 프론트에서 넘어온 공급가를 신뢰하지 않고, 서버에서 확정한 공급가로 세팅
                        detail.setSupplyPrice(resp.data.supplyPrice);

                        log.info("상품 정보 조회 성공: productId={}, supplyPrice={}",
                                detail.getProductId(), resp.data.supplyPrice);
                        return detail;

                    } catch (Exception e) {
                        log.error("상품 정보 조회 실패: productId={}, error={}",
                                detail.getProductId(), e.getMessage(), e);
                        throw new RuntimeException("상품 정보 조회 실패: " + detail.getProductId(), e);
                    }
                })
                .collect(Collectors.toList());
    }

    // 총액 계산 (supplyPrice null 가드)
    private long calculateTotalPrice(List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> orderDetails) {
        return orderDetails.stream()
                .filter(detail -> detail.getQuantity() > 0)
                .peek(d -> {
                    if (d.getSupplyPrice() == null) {
                        throw new IllegalArgumentException("공급가가 비어 있습니다. productId=" + d.getProductId());
                    }
                })
                .mapToLong(d -> (long) d.getQuantity() * d.getSupplyPrice())
                .sum();
    }

    // 발주 요청 유효성 검증
    private void validatePurchaseOrderRequest(PurchaseOrderRequestDto requestDto) {
        if (requestDto.getBranchId() == null) {
            throw new IllegalArgumentException("가맹점 ID는 필수입니다.");
        }
        validateBranchAccess(requestDto.getBranchId());

        if (requestDto.getOrderDetails() == null || requestDto.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("발주 상품을 선택해주세요.");
        }

        boolean hasValidQuantity = requestDto.getOrderDetails().stream()
                .anyMatch(detail -> detail.getQuantity() > 0);

        if (!hasValidQuantity) {
            throw new IllegalArgumentException("수량을 입력해주세요.");
        }
    }

    // 발주 요청 유효성 검증 (권한 검증 제외)
    private void validatePurchaseOrderRequestWithoutAuth(PurchaseOrderRequestDto requestDto) {
        if (requestDto.getBranchId() == null) {
            throw new IllegalArgumentException("가맹점 ID는 필수입니다.");
        }

        if (requestDto.getOrderDetails() == null || requestDto.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("발주 상품을 선택해주세요.");
        }

        boolean hasValidQuantity = requestDto.getOrderDetails().stream()
                .anyMatch(detail -> detail.getQuantity() > 0);

        if (!hasValidQuantity) {
            throw new IllegalArgumentException("수량을 입력해주세요.");
        }
    }

    // 중복 발주 방지 검증
    private void validateNoDuplicateOrder(PurchaseOrderRequestDto requestDto) {
        LocalDate today = LocalDate.now();

        List<PurchaseOrder> existingOrders = purchaseOrderRepository
                .findByBranchIdAndOrderDateAndOrderStatus(
                        requestDto.getBranchId(),
                        today,
                        OrderStatus.PENDING
                );

        if (!existingOrders.isEmpty()) {
            Set<Long> existingProductIds = existingOrders.stream()
                    .flatMap(order -> order.getOrderDetails().stream())
                    .map(PurchaseOrderDetail::getProductId)
                    .collect(Collectors.toSet());

            Set<Long> newProductIds = requestDto.getOrderDetails().stream()
                    .map(PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto::getProductId)
                    .collect(Collectors.toSet());

            boolean hasDuplicate = existingProductIds.stream().anyMatch(newProductIds::contains);
            if (hasDuplicate) {
                throw new IllegalArgumentException("이미 같은 상품으로 발주 요청이 진행 중입니다. 기존 발주를 확인해주세요.");
            }
        }
    }

    /**
     * 지점 접근 권한 검증
     * - 본사 관리자: 모든 지점 접근 가능
     * - 가맹점주/직원: 자신의 지점만 접근 가능
     */
    private void validateBranchAccess(Long branchId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                throw new SecurityException("인증되지 않은 사용자입니다.");
            }

            Object details = auth.getDetails();
            if (!(details instanceof Claims claims)) {
                throw new SecurityException("JWT 토큰 정보를 찾을 수 없습니다.");
            }

            Long employeeId = claims.get("employeeId", Long.class);
            String role = claims.get("role", String.class);

            if ("HQ_ADMIN".equals(role)) {
                log.info("본사 관리자 권한으로 모든 지점 접근 허용");
                return;
            }

            Employee employee = employeeRepository.findWithDispatchStatusesById(employeeId)
                    .orElseThrow(() -> new SecurityException("사용자 정보를 찾을 수 없습니다."));

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
        validateBranchAccess(branchId);

        if (branchId.equals(HEAD_OFFICE_BRANCH_ID)) {
            return getAllPurchaseOrders();
        } else {
            return getPurchaseOrdersByBranch(branchId);
        }
    }

    public List<PurchaseOrderListResponseDto> getPurchaseOrdersByBranch(Long branchId) {
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findByBranchId(branchId);
        return purchaseOrders.stream()
                .map(this::convertToListResponseDto)
                .collect(Collectors.toList());
    }

    public List<PurchaseOrderListResponseDto> getAllPurchaseOrders() {
        List<PurchaseOrder> purchaseOrders = purchaseOrderRepository.findAll();
        return purchaseOrders.stream()
                .filter(order -> order.getOrderStatus() != OrderStatus.CANCELLED)
                .map(this::convertToListResponseDto)
                .collect(Collectors.toList());
    }

    public PurchaseOrderResponseDto getPurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));

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

        for (PurchaseOrderDetail detail : orderDetails) {
            detail.setApprovedQuantity(detail.getQuantity());
        }
        orderDetails = purchaseOrderDetailRepository.saveAll(orderDetails);

        publishPurchaseOrderApprovedEvent(savedOrder, orderDetails);

        //[알림 - 발주 승인]
        List<DispatchStatus> employees = dispatchStatusRepository.findAllByBranchId(purchaseOrder.getBranchId());
        for(DispatchStatus ds : employees){
            SseNotificationResDto dto = SseNotificationResDto.orderStatusChanged(purchaseOrder.getBranchId(), ds.getEmployee().getEmail(), purchaseOrder.getId(), "APPROVED");
            sseAlarmService.publishNotification(dto);
        }

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

        // 예약재고 해제
        releaseHqStocks(savedOrder, orderDetails);

        //[알림 - 발주 반려]
        List<DispatchStatus> employees = dispatchStatusRepository.findAllByBranchId(purchaseOrder.getBranchId());
        for(DispatchStatus ds : employees){
            SseNotificationResDto dto = SseNotificationResDto.orderStatusChanged(purchaseOrder.getBranchId(), ds.getEmployee().getEmail(), purchaseOrder.getId(), "APPROVED");
            sseAlarmService.publishNotification(dto);
        }
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

        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(purchaseOrder);

        setApprovedQuantities(orderDetails, requestDto.getApprovedDetails());

        determineApprovalStatus(purchaseOrder, orderDetails);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderDetail> savedDetails = purchaseOrderDetailRepository.saveAll(orderDetails);

        // 부분 승인 시 거부된 수량만큼 예약재고 해제
        releasePartialHqStocks(savedOrder, savedDetails);

        publishPartialApprovedEvent(savedOrder, savedDetails);


        //[알림 - 발주 반려]
        List<DispatchStatus> employees = dispatchStatusRepository.findAllByBranchId(purchaseOrder.getBranchId());
        for(DispatchStatus ds : employees){
            SseNotificationResDto dto = SseNotificationResDto.orderStatusChanged(purchaseOrder.getBranchId(), ds.getEmployee().getEmail(), purchaseOrder.getId(), "PARTIALLY_APPROVED");
            sseAlarmService.publishNotification(dto);
        }
        return convertToResponseDto(savedOrder, savedDetails);
    }

    private void determineApprovalStatus(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        boolean allFullyApproved = orderDetails.stream()
                .allMatch(detail -> detail.getApprovedQuantity() == detail.getQuantity());

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

    private void setApprovedQuantities(List<PurchaseOrderDetail> orderDetails,
                                       List<PartialApproveRequestDto.PartialApproveDetailDto> approvedDetails) {

        for (PurchaseOrderDetail orderDetail : orderDetails) {
            PartialApproveRequestDto.PartialApproveDetailDto approvedDetail = approvedDetails.stream()
                    .filter(detail -> detail.getProductId().equals(orderDetail.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (approvedDetail != null) {
                if (approvedDetail.getApprovedQuantity() > orderDetail.getQuantity()) {
                    throw new IllegalArgumentException(
                            String.format("승인 수량(%d)이 요청 수량(%d)을 초과할 수 없습니다.",
                                    approvedDetail.getApprovedQuantity(), orderDetail.getQuantity()));
                }

                if (approvedDetail.getApprovedQuantity() <= 0) {
                    throw new IllegalArgumentException("승인 수량은 1개 이상이어야 합니다.");
                }

                orderDetail.setApprovedQuantity(approvedDetail.getApprovedQuantity());

                log.info("상품 부분 승인: productId={}, 요청수량={}, 승인수량={}",
                        orderDetail.getProductId(), orderDetail.getQuantity(), approvedDetail.getApprovedQuantity());
            } else {
                orderDetail.setApprovedQuantity(0);
                log.info("상품 승인 제외: productId={}, 요청수량={}, 승인수량=0",
                        orderDetail.getProductId(), orderDetail.getQuantity());
            }
        }
    }

    private PurchaseOrderListResponseDto convertToListResponseDto(PurchaseOrder purchaseOrder) {
        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(purchaseOrder);
        int productCount = orderDetails.size();

        String branchName = "지점-" + purchaseOrder.getBranchId();
        try {
            branchName = branchRepository.findById(purchaseOrder.getBranchId())
                    .map(branch -> branch.getName())
                    .orElse("지점-" + purchaseOrder.getBranchId());
        } catch (Exception e) {
            log.warn("지점 정보 조회 실패: branchId={}", purchaseOrder.getBranchId(), e);
        }

        return PurchaseOrderListResponseDto.builder()
                .purchaseOrderId(purchaseOrder.getId())
                .branchId(purchaseOrder.getBranchId())
                .branchName(branchName)
                .orderStatus(purchaseOrder.getOrderStatus())
                .totalPrice(purchaseOrder.getPrice())
                .productCount(productCount)
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
        String productName = getProductName(detail.getProductId());
        String categoryName = getCategoryName(detail.getProductId());

        return PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.builder()
                .purchaseOrderDetailId(detail.getId())
                .productId(detail.getProductId())
                .productName(productName)
                .categoryName(categoryName)
                .quantity(detail.getQuantity())
                .approvedQuantity(detail.getApprovedQuantity())
                .unitPrice(detail.getUnitPrice())
                .subtotalPrice(detail.getSubtotalPrice())
                .build();
    }

    // 언랩 + null 가드 추가
    private String getProductName(Long productId) {
        try {
            ResponseDto<OrderingInventoryClient.ProductResponseDto> resp = orderingInventoryClient.getProduct(productId);
            return (resp != null && resp.data != null && resp.data.name != null)
                    ? resp.data.name
                    : "상품 ID: " + productId;
        } catch (Exception e) {
            return "상품 ID: " + productId;
        }
    }

    private String getCategoryName(Long productId) {
        try {
            ResponseDto<OrderingInventoryClient.ProductResponseDto> resp = orderingInventoryClient.getProduct(productId);
            return (resp != null && resp.data != null && resp.data.categoryName != null)
                    ? resp.data.categoryName
                    : "미분류";
        } catch (Exception e) {
            return "미분류";
        }
    }

    // kafka 이벤트 발송
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

    private void publishPartialApprovedEvent(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        try {
            PurchaseOrderResponseDto event = convertToPartialApprovedResponseDto(purchaseOrder, orderDetails);
            purchaseOrderKafkaTemplate.send(PURCHASE_ORDER_APPROVED_TOPIC, event);
        } catch (Exception e) {
            log.error("발주 부분 승인 이벤트 발송 실패: purchaseOrderId={}", purchaseOrder.getId(), e);
        }
    }

    private PurchaseOrderResponseDto convertToPartialApprovedResponseDto(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        List<PurchaseOrderDetail> approvedDetails = orderDetails.stream()
                .filter(detail -> detail.getApprovedQuantity() > 0)
                .collect(Collectors.toList());

        List<PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto> approvedDetailDtos = approvedDetails.stream()
                .map(detail -> {
                    String productName = getProductName(detail.getProductId());
                    return PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.builder()
                            .purchaseOrderDetailId(detail.getId())
                            .productId(detail.getProductId())
                            .productName(productName)
                            .quantity(detail.getApprovedQuantity())
                            .approvedQuantity(detail.getApprovedQuantity())
                            .unitPrice(detail.getUnitPrice())
                            .subtotalPrice(detail.getApprovedQuantity() * detail.getUnitPrice())
                            .build();
                })
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

    private long calculatePartialApprovedTotalPrice(List<PurchaseOrderDetail> approvedDetails) {
        return approvedDetails.stream()
                .mapToLong(detail -> (long) detail.getApprovedQuantity() * detail.getUnitPrice())
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

        purchaseOrder.changeOrderStatus(OrderStatus.COMPLETED);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(savedOrder);

        publishInventoryIncreaseEvent(savedOrder, orderDetails);

        return convertToResponseDto(savedOrder, orderDetails);
    }

    // 발주 취소 (가맹점용)
    @Transactional
    public PurchaseOrderResponseDto cancelPurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));

        if (purchaseOrder.getOrderStatus() != OrderStatus.PENDING &&
                purchaseOrder.getOrderStatus() != OrderStatus.REJECTED) {
            throw new IllegalStateException("취소할 수 없는 발주 상태입니다. 현재 상태: " + purchaseOrder.getOrderStatus());
        }

        purchaseOrder.changeOrderStatus(OrderStatus.CANCELLED);
        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);

        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(savedOrder);
        
        // 예약재고 해제
        releaseHqStocks(savedOrder, orderDetails);
        
        return convertToResponseDto(savedOrder, orderDetails);
    }

    private void publishInventoryIncreaseEvent(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        try {
            PurchaseOrderResponseDto event = convertToCompletedResponseDto(purchaseOrder, orderDetails);
            purchaseOrderKafkaTemplate.send(PURCHASE_ORDER_COMPLETED_TOPIC, event);
        } catch (Exception e) {
            log.error("발주 입고 완료 이벤트 발송 실패: purchaseOrderId={}", purchaseOrder.getId(), e);
        }
    }

    private PurchaseOrderResponseDto convertToCompletedResponseDto(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        List<PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto> detailDtos = orderDetails.stream()
                .filter(detail -> detail.getApprovedQuantity() > 0)
                .map(detail -> {
                    String productName = getProductName(detail.getProductId());
                    return PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.builder()
                            .purchaseOrderDetailId(detail.getId())
                            .productId(detail.getProductId())
                            .productName(productName)
                            .quantity(detail.getApprovedQuantity())
                            .approvedQuantity(detail.getApprovedQuantity())
                            .unitPrice(detail.getUnitPrice())
                            .subtotalPrice(detail.getApprovedQuantity() * detail.getUnitPrice())
                            .build();
                })
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

    public Employee getOwner(){
        Long branchId = ChatUserService.getBranchIdFromToken();

        List<DispatchStatus> list = dispatchStatusRepository.findAllByBranchId(branchId);

        //  HQ_ADMIN,           /// 본사(본점) 관리자
        //    BRANCH_ADMIN,       /// 지점(직영) 관리자
        //    FRANCHISE_OWNER,
        return list.stream()
                .filter(em -> em.getEmployee().getAuthorityType().equals(AuthorityType.HQ_ADMIN)
                        || em.getEmployee().getAuthorityType().equals(AuthorityType.BRANCH_ADMIN)
                        || em.getEmployee().getAuthorityType().equals(AuthorityType.FRANCHISE_OWNER))
                .findFirst().orElseThrow(()-> new EntityNotFoundException("관리자가 존재하지 않습니다.")).getEmployee();

    }

    public Employee getEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("인증되지 않은 사용자입니다. 자동 발주에서는 사용할 수 없습니다.");
        }
        String email = auth.getName();
        return employeeRepository.findByEmail(email).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 직원입니다."));
    }
}
