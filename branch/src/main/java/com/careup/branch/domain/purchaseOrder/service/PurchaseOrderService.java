package com.careup.branch.domain.purchaseOrder.service;

import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.domain.branch.entity.Branch;
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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(PurchaseOrderRequestDto requestDto) {
        validatePurchaseOrderRequest(requestDto);
        validateNoDuplicateOrder(requestDto);
        List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> details = setSupplyPrices(requestDto.getOrderDetails());

        PurchaseOrder purchaseOrder = PurchaseOrder.builder()
                .branchId(requestDto.getBranchId())
                .orderStatus(OrderStatus.PENDING)
                .price(calculateTotalPrice(details))
                .build();

        List<PurchaseOrderDetail> orderDetails = details.stream()
                .map(detail -> {
                    String productName = getProductName(detail.getProductId());
                    return PurchaseOrderDetail.builder()
                            .purchaseOrder(purchaseOrder)
                            .productId(detail.getProductId())
                            .productName(productName)
                            .quantity(detail.getQuantity())
                            .approvedQuantity(0)
                            .unitPrice(detail.getSupplyPrice())
                            .subtotalPrice((long) detail.getQuantity() * detail.getSupplyPrice())
                            .build();
                })
                .toList();

        PurchaseOrder savedOrder = purchaseOrderRepository.save(purchaseOrder);
        orderDetails.forEach(d -> d.setPurchaseOrder(savedOrder));
        orderDetails.forEach(purchaseOrderDetailRepository::save);

        reserveHqStocksOrThrow(savedOrder, orderDetails, requestDto);

        Branch branch = branchRepository.findById(requestDto.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지점입니다."));
        SseNotificationResDto dto = SseNotificationResDto.orderStatusChanged(branch.getName(), purchaseOrder.getId(), "REQUESTED", HEAD_OFFICE_BRANCH_ID);
        sseAlarmService.publishNotification(dto);

        log.info("발주 생성 완료: {}", savedOrder.getId());
        return PurchaseOrderResponseDto.builder()
                .purchaseOrderId(savedOrder.getId())
                .branchId(savedOrder.getBranchId())
                .orderStatus(savedOrder.getOrderStatus())
                .totalPrice(savedOrder.getPrice())
                .createdAt(savedOrder.getCreatedAt())
                .build();
    }

    public PurchaseOrderResponseDto createAutoPurchaseOrder(PurchaseOrderRequestDto requestDto) {
        try {
            validatePurchaseOrderRequestWithoutAuth(requestDto);
            List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> details = setSupplyPrices(requestDto.getOrderDetails());

            TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
            txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

            AutoOrderContext context = txTemplate.execute(status -> {
                validateNoDuplicateOrder(requestDto);

                PurchaseOrder po = PurchaseOrder.builder()
                        .branchId(requestDto.getBranchId())
                        .orderStatus(OrderStatus.PENDING)
                        .price(calculateTotalPrice(details))
                        .build();

                PurchaseOrder savedOrder = purchaseOrderRepository.save(po);
                List<PurchaseOrderDetail> savedDetails = details.stream()
                        .filter(detail -> detail.getQuantity() > 0)
                        .map(d -> {
                            String productName = getProductName(d.getProductId());
                            return PurchaseOrderDetail.builder()
                                    .purchaseOrder(savedOrder)
                                    .productId(d.getProductId())
                                    .productName(productName)
                                    .quantity(d.getQuantity())
                                    .approvedQuantity(0)
                                    .unitPrice(d.getSupplyPrice())
                                    .subtotalPrice(d.getQuantity() * d.getSupplyPrice())
                                    .build();
                        })
                        .collect(Collectors.toList());
                purchaseOrderDetailRepository.saveAll(savedDetails);
                return new AutoOrderContext(savedOrder, savedDetails);
            });

            if (context == null) throw new RuntimeException("자동 발주 생성 중 알 수 없는 오류가 발생했습니다.");

            try {
                reserveHqStocksOrThrow(context.purchaseOrder(), context.orderDetails(), requestDto);
            } catch (RuntimeException ex) {
                txTemplate.execute(status -> {
                    PurchaseOrder persisted = purchaseOrderRepository.findById(context.purchaseOrder().getId()).orElse(null);
                    if (persisted != null) {
                        List<PurchaseOrderDetail> persistedDetails = purchaseOrderDetailRepository.findByPurchaseOrder(persisted);
                        purchaseOrderDetailRepository.deleteAll(persistedDetails);
                        purchaseOrderRepository.delete(persisted);
                    }
                    return null;
                });
                throw ex;
            }
            return convertToCompletedResponseDto(context.purchaseOrder(), context.orderDetails());
        } catch (SecurityException e) {
            throw new RuntimeException("자동 발주 생성 실패: " + e.getMessage(), e);
        }
    }

    private void reserveHqStocksOrThrow(PurchaseOrder savedOrder, List<PurchaseOrderDetail> orderDetails, PurchaseOrderRequestDto requestDto) {
        Long hqBranchId = 1L;
        Map<Long, Long> productIdToAttributeValueId = new HashMap<>();
        if (requestDto != null && requestDto.getOrderDetails() != null) {
            for (PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto d : requestDto.getOrderDetails()) {
                if (d.getAttributeValueId() != null) {
                    productIdToAttributeValueId.put(d.getProductId(), d.getAttributeValueId());
                }
            }
        }
        for (PurchaseOrderDetail detail : orderDetails) {
            try {
                OrderingInventoryClient.BranchProductResponseDto hqBp = null;
                Long attributeValueId = productIdToAttributeValueId.get(detail.getProductId());
                if (attributeValueId != null) {
                    List<OrderingInventoryClient.BranchProductResponseDto> hqProducts = orderingInventoryClient.getBranchProducts(hqBranchId);
                    hqBp = hqProducts.stream()
                            .filter(bp -> bp.productId != null && bp.productId.equals(detail.getProductId())
                                    && bp.attributeValueId != null && bp.attributeValueId.equals(attributeValueId))
                            .findFirst()
                            .orElse(null);
                    if (hqBp == null) {
                        log.warn("본사 재고를 찾을 수 없습니다 (attributeValueId로 검색): productId={}, attributeValueId={}",
                                detail.getProductId(), attributeValueId);
                    }
                }
                if (hqBp == null) {
                    hqBp = orderingInventoryClient.getBranchProduct(hqBranchId, detail.getProductId());
                }
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
                long availableQuantity = 0;
                try {
                    OrderingInventoryClient.BranchProductResponseDto hqBp = orderingInventoryClient.getBranchProduct(hqBranchId, detail.getProductId());
                    if (hqBp != null) {
                        if (hqBp.availableQuantity != null) availableQuantity = hqBp.availableQuantity;
                        else if (hqBp.stockQuantity != null) availableQuantity = hqBp.stockQuantity;
                    }
                } catch (Exception ex) {
                    log.warn("사용 가능한 재고 수량 조회 실패: productId={}", detail.getProductId(), ex);
                }
                String originalMessage = e.getMessage() != null ? e.getMessage() : "재고 확보 실패";
                String errorMessage = String.format("재고 확보 실패: productId=%d, 요청수량=%d, 최대가능수량=%d, 원인=%s",
                        detail.getProductId(), detail.getQuantity(), availableQuantity, originalMessage);
                throw new RuntimeException(errorMessage, e);
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
            }
        }
    }

    private void releasePartialHqStocks(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        Long hqBranchId = 1L;
        for (PurchaseOrderDetail detail : orderDetails) {
            long rejectedQuantity = detail.getQuantity() - detail.getApprovedQuantity();
            if (rejectedQuantity <= 0) continue;
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
                        detail.setSupplyPrice(resp.data.supplyPrice);
                        log.info("상품 정보 조회 성공: productId={}, supplyPrice={}", detail.getProductId(), resp.data.supplyPrice);
                        return detail;
                    } catch (Exception e) {
                        log.error("상품 정보 조회 실패: productId={}, error={}", detail.getProductId(), e.getMessage(), e);
                        throw new RuntimeException("상품 정보 조회 실패: " + detail.getProductId(), e);
                    }
                })
                .collect(Collectors.toList());
    }

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

    private void validatePurchaseOrderRequest(PurchaseOrderRequestDto requestDto) {
        if (requestDto.getBranchId() == null) throw new IllegalArgumentException("가맹점 ID는 필수입니다.");
        validateBranchAccess(requestDto.getBranchId());
        if (requestDto.getOrderDetails() == null || requestDto.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("발주 상품을 선택해주세요.");
        }
        boolean hasValidQuantity = requestDto.getOrderDetails().stream().anyMatch(detail -> detail.getQuantity() > 0);
        if (!hasValidQuantity) throw new IllegalArgumentException("수량을 입력해주세요.");
    }

    private void validatePurchaseOrderRequestWithoutAuth(PurchaseOrderRequestDto requestDto) {
        if (requestDto.getBranchId() == null) throw new IllegalArgumentException("가맹점 ID는 필수입니다.");
        if (requestDto.getOrderDetails() == null || requestDto.getOrderDetails().isEmpty()) {
            throw new IllegalArgumentException("발주 상품을 선택해주세요.");
        }
        boolean hasValidQuantity = requestDto.getOrderDetails().stream().anyMatch(detail -> detail.getQuantity() > 0);
        if (!hasValidQuantity) throw new IllegalArgumentException("수량을 입력해주세요.");
    }

    private void validateNoDuplicateOrder(PurchaseOrderRequestDto requestDto) {
        LocalDate today = LocalDate.now();
        List<PurchaseOrder> existingOrders = purchaseOrderRepository
                .findByBranchIdAndOrderDateAndOrderStatus(requestDto.getBranchId(), today, OrderStatus.PENDING);
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

    private void validateBranchAccess(Long branchId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) throw new SecurityException("인증되지 않은 사용자입니다.");
            Object details = auth.getDetails();
            if (!(details instanceof io.jsonwebtoken.Claims claims)) throw new SecurityException("JWT 토큰 정보를 찾을 수 없습니다.");
            Long employeeId = claims.get("employeeId", Long.class);
            String role = claims.get("role", String.class);
            if ("HQ_ADMIN".equals(role)) return;

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
                .filter(d -> d.getAssignedFrom().isBefore(now) || d.getAssignedFrom().isEqual(now))
                .filter(d -> d.getAssignedTo().isAfter(now) || d.getAssignedTo().isEqual(now))
                .map(d -> d.getBranch().getId())
                .distinct()
                .toList();
    }

    /** 목록 조회: 페이지네이션 + 배치 집계(상품수/지점명) */
    public Page<PurchaseOrderListResponseDto> getPurchaseOrders(Long branchId, Pageable pageable) {
        validateBranchAccess(branchId);

        Page<PurchaseOrder> page;
        if (branchId.equals(HEAD_OFFICE_BRANCH_ID)) {
            page = purchaseOrderRepository.findByOrderStatusNot(OrderStatus.CANCELLED, pageable);
        } else {
            page = purchaseOrderRepository.findByBranchIdAndOrderStatusNot(branchId, OrderStatus.CANCELLED, pageable);
        }

        List<PurchaseOrder> content = page.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, page.getTotalElements());
        }

        List<Long> orderIds = content.stream().map(PurchaseOrder::getId).toList();
        Map<Long, Integer> productCountMap = purchaseOrderDetailRepository.countByPurchaseOrderIds(orderIds)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> ((Number) r[1]).intValue()
                ));

        Set<Long> branchIds = content.stream().map(PurchaseOrder::getBranchId).collect(Collectors.toSet());
        Map<Long, String> branchNameMap = branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName));

        List<PurchaseOrderListResponseDto> result = content.stream()
                .map(po -> PurchaseOrderListResponseDto.builder()
                        .purchaseOrderId(po.getId())
                        .branchId(po.getBranchId())
                        .branchName(branchNameMap.getOrDefault(po.getBranchId(), "지점-" + po.getBranchId()))
                        .orderStatus(po.getOrderStatus())
                        .totalPrice(po.getPrice())
                        .productCount(productCountMap.getOrDefault(po.getId(), 0))
                        .createdAt(po.getCreatedAt())
                        .updatedAt(po.getUpdatedAt())
                        .build()
                )
                .toList();

        return new PageImpl<>(result, pageable, page.getTotalElements());
    }

    public PurchaseOrderResponseDto getPurchaseOrder(Long purchaseOrderId) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));
        validateBranchAccess(purchaseOrder.getBranchId());
        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(purchaseOrder);
        return convertToResponseDto(purchaseOrder, orderDetails);
    }

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

        Branch branch = branchRepository.findById(purchaseOrder.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지점입니다."));
        SseNotificationResDto branchDto = SseNotificationResDto.orderStatusChanged(branch.getName(), purchaseOrder.getId(), "APPROVED", purchaseOrder.getBranchId());
        SseNotificationResDto headOfficeDto = SseNotificationResDto.orderStatusChanged(branch.getName(), purchaseOrder.getId(), "승인", HEAD_OFFICE_BRANCH_ID);
        sseAlarmService.publishNotification(branchDto);
        sseAlarmService.publishNotification(headOfficeDto);

        return convertToResponseDto(savedOrder, orderDetails);
    }

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
        releaseHqStocks(savedOrder, orderDetails);

        Branch branch = branchRepository.findById(purchaseOrder.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지점입니다."));
        SseNotificationResDto dto = SseNotificationResDto.orderStatusChanged(branch.getName(), purchaseOrder.getId(), "REJECTED", purchaseOrder.getBranchId());
        SseNotificationResDto headOfficeDto = SseNotificationResDto.orderStatusChanged(branch.getName(), purchaseOrder.getId(), "반려", HEAD_OFFICE_BRANCH_ID);
        sseAlarmService.publishNotification(dto);
        sseAlarmService.publishNotification(headOfficeDto);

        return convertToResponseDto(savedOrder, orderDetails);
    }

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

        releasePartialHqStocks(savedOrder, savedDetails);
        publishPartialApprovedEvent(savedOrder, savedDetails);

        Branch branch = branchRepository.findById(purchaseOrder.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지점입니다."));
        SseNotificationResDto dto = SseNotificationResDto.orderStatusChanged(branch.getName(), purchaseOrder.getId(), "PARTIALLY_APPROVED", purchaseOrder.getBranchId());
        SseNotificationResDto headOfficeDto = SseNotificationResDto.orderStatusChanged(branch.getName(), purchaseOrder.getId(), "부분 승인", HEAD_OFFICE_BRANCH_ID);
        sseAlarmService.publishNotification(dto);
        sseAlarmService.publishNotification(headOfficeDto);

        return convertToResponseDto(savedOrder, savedDetails);
    }

    private void determineApprovalStatus(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
        boolean allFullyApproved = orderDetails.stream().allMatch(d -> d.getApprovedQuantity() == d.getQuantity());
        boolean allRejected = orderDetails.stream().allMatch(d -> d.getApprovedQuantity() == 0);
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
                    .filter(d -> d.getProductId().equals(orderDetail.getProductId()))
                    .findFirst().orElse(null);
            if (approvedDetail != null) {
                if (approvedDetail.getApprovedQuantity() > orderDetail.getQuantity()) {
                    throw new IllegalArgumentException(String.format("승인 수량(%d)이 요청 수량(%d)을 초과할 수 없습니다.",
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
                log.info("상품 승인 제외: productId={}, 요청수량={}, 승인수량=0", orderDetail.getProductId(), orderDetail.getQuantity());
            }
        }
    }

    private PurchaseOrderListResponseDto convertToListResponseDto(PurchaseOrder purchaseOrder) {
        List<PurchaseOrderDetail> orderDetails = purchaseOrderDetailRepository.findByPurchaseOrder(purchaseOrder);
        int productCount = orderDetails.size();

        String branchName = "지점-" + purchaseOrder.getBranchId();
        try {
            branchName = branchRepository.findById(purchaseOrder.getBranchId())
                    .map(Branch::getName)
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
                        .map(detail -> convertToDetailResponseDto(detail, purchaseOrder.getOrderStatus()))
                        .collect(Collectors.toList()))
                .build();
    }

    private PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto convertToDetailResponseDto(PurchaseOrderDetail detail, OrderStatus orderStatus) {
        String productName = getProductName(detail.getProductId());
        String categoryName = getCategoryName(detail.getProductId());
        List<PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.AttributeInfo> attributes = getProductAttributes(detail.getProductId());
        int approvedQuantity = (orderStatus == OrderStatus.PENDING) ? 0 : detail.getApprovedQuantity();

        return PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.builder()
                .purchaseOrderDetailId(detail.getId())
                .productId(detail.getProductId())
                .productName(productName)
                .categoryName(categoryName)
                .quantity(detail.getQuantity())
                .approvedQuantity(approvedQuantity)
                .unitPrice(detail.getUnitPrice())
                .subtotalPrice(detail.getSubtotalPrice())
                .attributes(attributes)
                .build();
    }

    private List<PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.AttributeInfo> getProductAttributes(Long productId) {
        try {
            ResponseDto<List<OrderingInventoryClient.ProductAttributeValueResponseDto>> resp =
                    orderingInventoryClient.getProductAttributeValues(productId);
            if (resp == null || resp.data == null || resp.data.isEmpty()) {
                return new ArrayList<>();
            }
            Map<Long, PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.AttributeInfo> attributeMap = new HashMap<>();
            for (OrderingInventoryClient.ProductAttributeValueResponseDto pav : resp.data) {
                Long typeId = pav.attributeTypeId;
                if (typeId == null) continue;
                if (!attributeMap.containsKey(typeId)) {
                    attributeMap.put(typeId, PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.AttributeInfo.builder()
                            .attributeTypeId(pav.attributeTypeId)
                            .attributeTypeName(pav.attributeTypeName)
                            .attributeValueId(pav.attributeValueId)
                            .attributeValueName(pav.displayName)
                            .build());
                }
            }
            return attributeMap.values().stream().limit(2).collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String getProductName(Long productId) {
        try {
            ResponseDto<OrderingInventoryClient.ProductResponseDto> resp = orderingInventoryClient.getProduct(productId);
            return (resp != null && resp.data != null && resp.data.name != null) ? resp.data.name : "상품 ID: " + productId;
        } catch (Exception e) { return "상품 ID: " + productId; }
    }

    private String getCategoryName(Long productId) {
        try {
            ResponseDto<OrderingInventoryClient.ProductResponseDto> resp = orderingInventoryClient.getProduct(productId);
            return (resp != null && resp.data != null && resp.data.categoryName != null) ? resp.data.categoryName : "미분류";
        } catch (Exception e) { return "미분류"; }
    }

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
                    String categoryName = getCategoryName(detail.getProductId());
                    List<PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.AttributeInfo> attributes = getProductAttributes(detail.getProductId());
                    return PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.builder()
                            .purchaseOrderDetailId(detail.getId())
                            .productId(detail.getProductId())
                            .productName(productName)
                            .categoryName(categoryName)
                            .quantity(detail.getApprovedQuantity())
                            .approvedQuantity(detail.getApprovedQuantity())
                            .unitPrice(detail.getUnitPrice())
                            .subtotalPrice(detail.getApprovedQuantity() * detail.getUnitPrice())
                            .attributes(attributes)
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
        return approvedDetails.stream().mapToLong(d -> (long) d.getApprovedQuantity() * d.getUnitPrice()).sum();
    }

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
                    String categoryName = getCategoryName(detail.getProductId());
                    List<PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.AttributeInfo> attributes = getProductAttributes(detail.getProductId());
                    return PurchaseOrderResponseDto.PurchaseOrderDetailResponseDto.builder()
                            .purchaseOrderDetailId(detail.getId())
                            .productId(detail.getProductId())
                            .productName(productName)
                            .categoryName(categoryName)
                            .attributes(attributes)
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

    public Employee getOwner() {
        Long branchId = ChatUserService.getBranchIdFromToken();
        List<DispatchStatus> list = dispatchStatusRepository.findAllByBranchId(branchId);
        return list.stream()
                .filter(em -> em.getEmployee().getAuthorityType().equals(AuthorityType.HQ_ADMIN)
                        || em.getEmployee().getAuthorityType().equals(AuthorityType.BRANCH_ADMIN)
                        || em.getEmployee().getAuthorityType().equals(AuthorityType.FRANCHISE_OWNER))
                .findFirst().orElseThrow(() -> new EntityNotFoundException("관리자가 존재하지 않습니다.")).getEmployee();
    }

    public Employee getEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new SecurityException("인증되지 않은 사용자입니다. 자동 발주에서는 사용할 수 없습니다.");
        String email = auth.getName();
        return employeeRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 직원입니다."));
    }

    private static class AutoOrderContext {
        private final PurchaseOrder purchaseOrder;
        private final List<PurchaseOrderDetail> orderDetails;
        private AutoOrderContext(PurchaseOrder purchaseOrder, List<PurchaseOrderDetail> orderDetails) {
            this.purchaseOrder = purchaseOrder; this.orderDetails = orderDetails;
        }
        public PurchaseOrder purchaseOrder() { return purchaseOrder; }
        public List<PurchaseOrderDetail> orderDetails() { return orderDetails; }
    }
}
