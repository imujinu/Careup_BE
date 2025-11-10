package com.careup.branch.domain.purchaseOrder.controller;

import com.careup.branch.domain.purchaseOrder.dto.*;
import com.careup.branch.domain.purchaseOrder.dto.HQStatisticsResponseDto;
import com.careup.branch.domain.purchaseOrder.dto.PartialApproveRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderListResponseDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.careup.branch.domain.purchaseOrder.service.PurchaseOrderExcelService;
import com.careup.branch.domain.purchaseOrder.service.PurchaseOrderService;
import com.careup.branch.domain.purchaseOrder.service.PurchaseOrderStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final PurchaseOrderExcelService excelService;
    private final PurchaseOrderStatisticsService statisticsService;

    @PostMapping
    public ResponseEntity<PurchaseOrderResponseDto> createPurchaseOrder(@RequestBody PurchaseOrderRequestDto requestDto) {
        PurchaseOrderResponseDto createdOrder = purchaseOrderService.createPurchaseOrder(requestDto);
        return ResponseEntity.ok(createdOrder);
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<Page<PurchaseOrderListResponseDto>> getPurchaseOrders(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        Pageable pageable = toPageable(page, size, sort);
        Page<PurchaseOrderListResponseDto> purchaseOrders = purchaseOrderService.getPurchaseOrders(branchId, pageable);
        return ResponseEntity.ok(purchaseOrders);
    }

    @GetMapping("/{purchaseOrderId}")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto purchaseOrder = purchaseOrderService.getPurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(purchaseOrder);
    }

    @PostMapping("/{purchaseOrderId}/approve")
    public ResponseEntity<PurchaseOrderResponseDto> approvePurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto approvedOrder = purchaseOrderService.approvePurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(approvedOrder);
    }

    @PostMapping("/{purchaseOrderId}/reject")
    public ResponseEntity<PurchaseOrderResponseDto> rejectPurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto rejectedOrder = purchaseOrderService.rejectPurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(rejectedOrder);
    }

    @PostMapping("/{purchaseOrderId}/partial-approve")
    public ResponseEntity<PurchaseOrderResponseDto> partialApprovePurchaseOrder(
            @PathVariable Long purchaseOrderId,
            @RequestBody PartialApproveRequestDto requestDto
    ) {
        PurchaseOrderResponseDto partialApprovedOrder = purchaseOrderService.partialApprovePurchaseOrder(purchaseOrderId, requestDto);
        return ResponseEntity.ok(partialApprovedOrder);
    }

    @PostMapping("/{purchaseOrderId}/ship")
    public ResponseEntity<PurchaseOrderResponseDto> shipPurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto shippedOrder = purchaseOrderService.shipPurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(shippedOrder);
    }

    @PostMapping("/{purchaseOrderId}/complete")
    public ResponseEntity<PurchaseOrderResponseDto> completePurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto completedOrder = purchaseOrderService.completePurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(completedOrder);
    }

    @PostMapping("/{purchaseOrderId}/cancel")
    public ResponseEntity<PurchaseOrderResponseDto> cancelPurchaseOrder(@PathVariable Long purchaseOrderId) {
        PurchaseOrderResponseDto cancelledOrder = purchaseOrderService.cancelPurchaseOrder(purchaseOrderId);
        return ResponseEntity.ok(cancelledOrder);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam Long branchId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        byte[] excelBytes = excelService.exportToExcel(branchId, startDate, endDate);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "purchase_orders_" + timestamp + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    @GetMapping("/{purchaseOrderId}/export/excel")
    public ResponseEntity<byte[]> exportSingleOrderToExcel(@PathVariable Long purchaseOrderId) {
        byte[] excelBytes = excelService.exportSingleOrderToExcel(purchaseOrderId);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "purchase_order_" + purchaseOrderId + "_" + timestamp + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    private Pageable toPageable(int page, int size, String sortParam) {
        try {
            String[] parts = sortParam.split(",");
            String prop = parts[0].trim();
            Sort.Direction dir = (parts.length > 1 ? Sort.Direction.fromString(parts[1].trim()) : Sort.Direction.DESC);
            return PageRequest.of(page, size, Sort.by(new Sort.Order(dir, prop)));
        } catch (Exception e) {
            return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        }
    }


    // 본사용 발주 통계 조회 (전체)
    @GetMapping("/statistics/hq")
    public ResponseEntity<HQStatisticsResponseDto> getHQStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : null;

        HQStatisticsResponseDto statistics = statisticsService.getHQStatistics(start, end);
        return ResponseEntity.ok(statistics);
    }

    // 본사용 전체현황 통계 조회
    @GetMapping("/statistics/hq/overall")
    public ResponseEntity<HQStatisticsResponseDto.OverallStatistics> getHQOverallStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : null;

        HQStatisticsResponseDto.OverallStatistics statistics = statisticsService.getHQOverallStatistics(start, end);
        return ResponseEntity.ok(statistics);
    }

    // 본사용 상태별 통계 조회
    @GetMapping("/statistics/hq/status")
    public ResponseEntity<List<HQStatisticsResponseDto.StatusStatistics>> getHQStatusStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : null;

        List<HQStatisticsResponseDto.StatusStatistics> statistics = statisticsService.getHQStatusStatistics(start, end);
        return ResponseEntity.ok(statistics);
    }

    // 본사용 지점별 통계 조회
    @GetMapping("/statistics/hq/branch")
    public ResponseEntity<List<HQStatisticsResponseDto.BranchStatistics>> getHQBranchStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : null;

        List<HQStatisticsResponseDto.BranchStatistics> statistics = statisticsService.getHQBranchStatistics(start, end);
        return ResponseEntity.ok(statistics);
    }

    // 본사용 상품별 통계 조회
    @GetMapping("/statistics/hq/product")
    public ResponseEntity<List<HQStatisticsResponseDto.ProductStatistics>> getHQProductStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : null;

        List<HQStatisticsResponseDto.ProductStatistics> statistics = statisticsService.getHQProductStatistics(start, end);
        return ResponseEntity.ok(statistics);
    }

    // 가맹점용 발주 통계 조회
    @GetMapping("/statistics/franchise/{branchId}")
    public ResponseEntity<HQStatisticsResponseDto.FranchiseStatistics> getFranchiseStatistics(
            @PathVariable Long branchId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : null;

        HQStatisticsResponseDto.FranchiseStatistics statistics = statisticsService.getFranchiseStatistics(branchId, start, end);
        return ResponseEntity.ok(statistics);
    }

    // 가맹점용 상품별 발주 통계 조회
    @GetMapping("/statistics/franchise/{branchId}/product")
    public ResponseEntity<List<HQStatisticsResponseDto.ProductStatistics>> getFranchiseProductStatistics(
            @PathVariable Long branchId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        LocalDate start = (startDate != null) ? LocalDate.parse(startDate) : null;
        LocalDate end = (endDate != null) ? LocalDate.parse(endDate) : null;

        List<HQStatisticsResponseDto.ProductStatistics> statistics = statisticsService.getFranchiseProductStatistics(branchId, start, end);
        return ResponseEntity.ok(statistics);
    }
}