package com.careup.branch.domain.purchaseOrder.service;

import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrder;
import com.careup.branch.domain.purchaseOrder.entity.PurchaseOrderDetail;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderDetailRepository;
import com.careup.branch.domain.purchaseOrder.repository.PurchaseOrderRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderExcelService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderDetailRepository purchaseOrderDetailRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final OrderingInventoryClient orderingInventoryClient;

    /**
     * 발주 내역을 엑셀 파일로 생성
     *
     * @param branchId 지점 ID (필수, 1이면 전체 발주, 그 외는 해당 지점만)
     * @return Excel 파일의 바이트 배열
     */
    public byte[] exportToExcel(Long branchId, String startDate, String endDate) {
        // 지점 접근 권한 검증
        validateBranchAccess(branchId);
        try {
            // 발주 목록 조회
            List<PurchaseOrder> orders = getFilteredOrders(branchId, startDate, endDate);

            // 엑셀 생성
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("발주내역");

            // 스타일 설정
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            // 헤더 작성
            createHeaderRow(sheet, headerStyle);

            // 데이터 작성
            int rowNum = 1;
            for (PurchaseOrder order : orders) {
                List<PurchaseOrderDetail> details = purchaseOrderDetailRepository.findByPurchaseOrder(order);

                if (details.isEmpty()) {
                    // 상세 내역이 없는 경우 발주 정보만 표시
                    createOrderRow(sheet, rowNum++, order, null, dataStyle, numberStyle);
                } else {
                    // 상세 내역이 있는 경우 각 상품별로 행 생성
                    for (PurchaseOrderDetail detail : details) {
                        createOrderRow(sheet, rowNum++, order, detail, dataStyle, numberStyle);
                    }
                }
            }

            // 같은 발주번호의 셀 병합
            mergeCellsForSameOrder(sheet, orders);

            // 열 너비 자동 조정
            for (int i = 0; i < 15; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
            }

            // ByteArray로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("엑셀 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 발주 내역 엑셀 파일로 생성
     */
    public byte[] exportSingleOrderToExcel(Long purchaseOrderId) {
        try {
            // 발주 조회
            PurchaseOrder order = purchaseOrderRepository.findById(purchaseOrderId)
                    .orElseThrow(() -> new RuntimeException("발주를 찾을 수 없습니다: " + purchaseOrderId));

            // 지점 접근 권한 검증
            validateBranchAccess(order.getBranchId());

            // 엑셀 생성
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("발주내역");

            // 스타일 설정
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            // 헤더 작성
            createHeaderRow(sheet, headerStyle);

            // 상세 내역 조회
            List<PurchaseOrderDetail> details = purchaseOrderDetailRepository.findByPurchaseOrder(order);

            // 데이터 작성
            int rowNum = 1;
            if (details.isEmpty()) {
                // 상세 내역이 없는 경우 발주 정보만 표시
                createOrderRow(sheet, rowNum++, order, null, dataStyle, numberStyle);
            } else {
                // 상세 내역이 있는 경우 각 상품별로 행 생성
                for (PurchaseOrderDetail detail : details) {
                    createOrderRow(sheet, rowNum++, order, detail, dataStyle, numberStyle);
                }
            }

            if (details.size() > 1) {
                int startRow = 1;
                int endRow = startRow + details.size() - 1;

                int[] mergeColumns = {0, 1, 2, 3, 4, 5};

                for (int col : mergeColumns) {
                    CellRangeAddress cellRangeAddress = new CellRangeAddress(startRow, endRow, col, col);
                    sheet.addMergedRegion(cellRangeAddress);

                    Cell mergedCell = sheet.getRow(startRow).getCell(col);
                    if (mergedCell != null) {
                        CellStyle style = mergedCell.getCellStyle();
                        style.setVerticalAlignment(VerticalAlignment.CENTER);
                        style.setAlignment(HorizontalAlignment.CENTER);
                        mergedCell.setCellStyle(style);
                    }
                }
            }

            for (int i = 0; i < 15; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("엑셀 파일 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 필터링된 발주 목록 조회
     */
    private List<PurchaseOrder> getFilteredOrders(Long branchId, String startDate, String endDate) {
        if (branchId == null) {
            throw new IllegalArgumentException("지점 ID는 필수입니다.");
        }

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (startDate != null && endDate != null) {
            startDateTime = java.time.LocalDate.parse(startDate).atStartOfDay();
            endDateTime = java.time.LocalDate.parse(endDate).atTime(java.time.LocalTime.MAX);
        }

        // branchId가 1이면 전체 발주 조회 (본사용)
        if (branchId == 1L) {
            if (startDateTime != null && endDateTime != null) {
                return purchaseOrderRepository.findByCreatedAtBetween(startDateTime, endDateTime);
            } else {
                return purchaseOrderRepository.findAll();
            }
        } else {
            // 특정 지점의 발주만 조회 (가맹점용)
            if (startDateTime != null && endDateTime != null) {
                return purchaseOrderRepository.findByBranchIdAndCreatedAtBetween(branchId, startDateTime, endDateTime);
            } else {
                return purchaseOrderRepository.findByBranchId(branchId);
            }
        }
    }

    /**
     * 헤더 행 생성
     */
    private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "발주번호", "지점명", "상태", "총금액(원)", "생성일시", "수정일시",
                "상품명", "옵션1", "옵션명1", "옵션2", "옵션명2",
                "상품수량", "승인수량", "단가(원)", "소계(원)"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * 데이터 행 생성
     */
    private void createOrderRow(Sheet sheet, int rowNum, PurchaseOrder order,
                                PurchaseOrderDetail detail, CellStyle dataStyle, CellStyle numberStyle) {
        Row row = sheet.createRow(rowNum);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 발주 기본 정보
        createCell(row, 0, "PO-" + String.format("%06d", order.getId()), dataStyle);  // 발주번호
        createCell(row, 1, getBranchName(order.getBranchId()), dataStyle);  // 지점명
        createCell(row, 2, getOrderStatusKorean(order.getOrderStatus().name()), dataStyle);  // 상태
        createCell(row, 3, order.getPrice(), numberStyle);  // 총금액
        createCell(row, 4, order.getCreatedAt().format(formatter), dataStyle);  // 생성일시
        createCell(row, 5, order.getUpdatedAt().format(formatter), dataStyle);  // 수정일시

        // 상세 정보 (있는 경우)
        if (detail != null) {
            createCell(row, 6, getProductName(detail.getProductId()), dataStyle);  // 상품명
            
            // 속성 정보 조회 및 표시
            List<OrderingInventoryClient.ProductAttributeValueResponseDto> attributes = getProductAttributes(detail.getProductId());
            String option1 = "-";
            String optionName1 = "-";
            String option2 = "-";
            String optionName2 = "-";
            
            if (attributes != null && !attributes.isEmpty()) {
                // 속성 타입별로 그룹화
                Map<Long, OrderingInventoryClient.ProductAttributeValueResponseDto> attributeMap = new HashMap<>();
                for (OrderingInventoryClient.ProductAttributeValueResponseDto pav : attributes) {
                    Long typeId = pav.attributeTypeId;
                    if (typeId != null && !attributeMap.containsKey(typeId)) {
                        attributeMap.put(typeId, pav);
                    }
                }
                
                // 최대 2개까지만 사용
                List<OrderingInventoryClient.ProductAttributeValueResponseDto> sortedAttributes = 
                        new ArrayList<>(attributeMap.values());
                
                if (sortedAttributes.size() > 0) {
                    OrderingInventoryClient.ProductAttributeValueResponseDto attr1 = sortedAttributes.get(0);
                    option1 = attr1.attributeTypeName != null ? attr1.attributeTypeName : "-";
                    optionName1 = attr1.displayName != null ? attr1.displayName : "-";
                }
                
                if (sortedAttributes.size() > 1) {
                    OrderingInventoryClient.ProductAttributeValueResponseDto attr2 = sortedAttributes.get(1);
                    option2 = attr2.attributeTypeName != null ? attr2.attributeTypeName : "-";
                    optionName2 = attr2.displayName != null ? attr2.displayName : "-";
                }
            }
            
            createCell(row, 7, option1, dataStyle);  // 옵션1
            createCell(row, 8, optionName1, dataStyle);  // 옵션명1
            createCell(row, 9, option2, dataStyle);  // 옵션2
            createCell(row, 10, optionName2, dataStyle);  // 옵션명2
            createCell(row, 11, detail.getQuantity(), numberStyle);  // 상품수량
            createCell(row, 12, detail.getApprovedQuantity(), numberStyle);  // 승인수량
            createCell(row, 13, detail.getUnitPrice(), numberStyle);  // 단가
            createCell(row, 14, detail.getSubtotalPrice(), numberStyle);  // 소계
        } else {
            // 상세 정보가 없는 경우 빈 셀
            for (int i = 6; i <= 14; i++) {
                createCell(row, i, "-", dataStyle);
            }
        }
    }

    /**
     * 셀 생성
     */
    private void createCell(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);

        if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }

        cell.setCellStyle(style);
    }

    /**
     * 헤더 스타일
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 배경색 (회색)
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 테두리
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 가운데 정렬
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 폰트 (굵게)
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    /**
     * 데이터 스타일
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 테두리
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 가운데 정렬
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * 숫자 스타일
     */
    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 테두리
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 오른쪽 정렬
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 숫자 포맷 (천단위 콤마)
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));

        return style;
    }

    /**
     * 지점 ID를 지점명으로 변환
     */
    private String getBranchName(Long branchId) {
        if (branchId == null) {
            return "알 수 없음";
        }

        try {
            Branch branch = branchRepository.findById(branchId).orElse(null);
            if (branch != null) {
                return branch.getName();
            }
        } catch (Exception e) {
            log.warn("지점명 조회 실패 - error: {}", e.getMessage());
        }

        return "지점 " + branchId;
    }

    /**
     * 주문 상태를 한글로 변환
     */
    private String getOrderStatusKorean(String status) {
        switch (status) {
            case "PENDING":
                return "대기중";
            case "APPROVED":
                return "승인됨";
            case "REJECTED":
                return "반려됨";
            case "PARTIAL":
                return "부분승인";
            case "SHIPPED":
                return "배송중";
            case "COMPLETED":
                return "완료됨";
            default:
                return status;
        }
    }

    /**
     * 상품 ID를 상품명으로 변환
     */
    private String getProductName(Long productId) {
        if (productId == null) {
            return "알 수 없음";
        }

        try {
            // Feign 응답(ResponseDto<ProductResponseDto>)에서 .data()로 언랩
            OrderingInventoryClient.ProductResponseDto product =
                    orderingInventoryClient.getProduct(productId).data();
            if (product != null && product.name != null) {
                return product.name;
            }
        } catch (Exception e) {
            log.warn("상품명 조회 실패 - error: {}", e.getMessage());
        }
        return "상품 " + productId;
    }

    /**
     * 상품 속성 정보 조회
     */
    private List<OrderingInventoryClient.ProductAttributeValueResponseDto> getProductAttributes(Long productId) {
        if (productId == null) {
            return new ArrayList<>();
        }

        try {
            OrderingInventoryClient.ResponseDto<List<OrderingInventoryClient.ProductAttributeValueResponseDto>> resp =
                    orderingInventoryClient.getProductAttributeValues(productId);
            if (resp != null && resp.data != null) {
                return resp.data;
            }
        } catch (Exception e) {
            log.warn("상품 속성 정보 조회 실패 - productId: {}, error: {}", productId, e.getMessage());
        }
        return new ArrayList<>();
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
            Object details = auth.getDetails();
            if (!(details instanceof Claims claims)) {
                throw new SecurityException("JWT 토큰 정보를 찾을 수 없습니다.");
            }

            Long employeeId = claims.get("employeeId", Long.class);
            String role = claims.get("role", String.class);

            // 3. 본사 관리자는 모든 지점 접근 가능
            if ("HQ_ADMIN".equals(role)) {
                return;
            }

            // 4. 가맹점주/직원인 경우 자신의 지점만 접근 가능
            Employee employee = employeeRepository.findWithDispatchStatusesById(employeeId)
                    .orElseThrow(() -> new SecurityException("사용자 정보를 찾을 수 없습니다."));

            // 5. 현재 활성화된 지점 배치 확인
            List<Long> accessibleBranchIds = getAccessibleBranchIds(employee);

            if (!accessibleBranchIds.contains(branchId)) {
                throw new SecurityException("해당 지점의 엑셀 다운로드 권한이 없습니다.");
            }

        } catch (Exception e) {
            throw new SecurityException("엑셀 다운로드 지점 접근 권한 검증에 실패했습니다: " + e.getMessage());
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

    /**
     * 같은 발주번호의 셀들을 병합
     * 발주번호, 지점명, 상태, 총금액, 생성일시, 수정일시 컬럼을 병합
     */
    private void mergeCellsForSameOrder(Sheet sheet, List<PurchaseOrder> orders) {
        int currentRow = 1; // 헤더 다음부터 시작

        for (PurchaseOrder order : orders) {
            List<PurchaseOrderDetail> details = purchaseOrderDetailRepository.findByPurchaseOrder(order);

            if (details.isEmpty()) {
                // 상세 내역이 없는 경우
                currentRow++;
            } else {
                // 상세 내역이 있는 경우
                int startRow = currentRow;
                int endRow = currentRow + details.size() - 1;

                // 같은 발주번호의 행이 2개 이상인 경우에만 병합
                if (endRow > startRow) {
                    // 병합할 컬럼들: 발주번호(0), 지점명(1), 상태(2), 총금액(3), 생성일시(4), 수정일시(5)
                    int[] mergeColumns = {0, 1, 2, 3, 4, 5};

                    for (int col : mergeColumns) {
                        CellRangeAddress cellRangeAddress = new CellRangeAddress(startRow, endRow, col, col);
                        sheet.addMergedRegion(cellRangeAddress);

                        // 병합된 셀의 정렬을 중앙으로 설정
                        Cell mergedCell = sheet.getRow(startRow).getCell(col);
                        if (mergedCell != null) {
                            CellStyle style = mergedCell.getCellStyle();
                            style.setVerticalAlignment(VerticalAlignment.CENTER);
                            style.setAlignment(HorizontalAlignment.CENTER);
                            mergedCell.setCellStyle(style);
                        }
                    }
                }
                currentRow = endRow + 1;
            }
        }
    }
}
