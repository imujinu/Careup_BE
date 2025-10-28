package com.careup.branch.domain.branch.service;

import com.careup.branch.domain.branch.client.OrderClient;
import com.careup.branch.domain.branch.dto.royalty.OrderSalesResponseDto;
import com.careup.branch.domain.branch.entity.Royalty;
import com.careup.branch.domain.branch.entity.SettlementStatus;
import com.careup.branch.domain.branch.repository.RoyaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoyaltyExcelService {

    private final RoyaltyRepository royaltyRepository;
    private final OrderClient orderClient;

    /**
     * 로열티 내역을 엑셀 파일로 생성
     *
     * @param branchId 지점 ID (null이면 전체 로열티 조회)
     * @param status 정산 상태 (null이면 전체 상태 조회)
     * @param startMonth 시작 연월 (yyyyMM 형식, null 가능)
     * @param endMonth 종료 연월 (yyyyMM 형식, null 가능)
     * @return Excel 파일의 바이트 배열
     */
    public byte[] exportToExcel(Long branchId, SettlementStatus status, String startMonth, String endMonth) {
        try {
            // 로열티 목록 조회
            List<Royalty> royalties = getFilteredRoyalties(branchId, status, startMonth, endMonth);

            // 엑셀 생성
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("로열티내역");

            // 스타일 설정
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // 제목 작성 (1행)
            createTitleRow(sheet, titleStyle, branchId, status, startMonth, endMonth);

            // 헤더 작성 (2행)
            createHeaderRow(sheet, headerStyle);

            // 데이터 작성 (3행부터)
            int rowNum = 2;
            for (Royalty royalty : royalties) {
                createRoyaltyRow(sheet, rowNum++, royalty, dataStyle, numberStyle, percentStyle, dateStyle);
            }

            // 열 너비 자동 조정
            for (int i = 0; i < 13; i++) {
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
     * 특정 지점의 로열티 내역 엑셀 파일로 생성
     *
     * @param branchId 지점 ID
     * @return Excel 파일의 바이트 배열
     */
    public byte[] exportBranchRoyaltyToExcel(Long branchId) {
        try {
            // 특정 지점의 로열티 목록 조회
            List<Royalty> royalties = royaltyRepository.findByBranchIdOrderByApplicableMonthDesc(branchId);

            if (royalties.isEmpty()) {
                throw new IllegalArgumentException("해당 지점의 로열티 내역이 없습니다. branchId: " + branchId);
            }

            // 엑셀 생성
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("로열티내역");

            // 스타일 설정
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // 제목 작성 (1행)
            String branchName = royalties.get(0).getBranch().getName();
            createBranchTitleRow(sheet, titleStyle, branchId, branchName);

            // 헤더 작성 (2행)
            createHeaderRow(sheet, headerStyle);

            // 데이터 작성 (3행부터)
            int rowNum = 2;
            for (Royalty royalty : royalties) {
                createRoyaltyRow(sheet, rowNum++, royalty, dataStyle, numberStyle, percentStyle, dateStyle);
            }

            // 열 너비 자동 조정
            for (int i = 0; i < 13; i++) {
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
     * 필터링된 로열티 목록 조회
     */
    private List<Royalty> getFilteredRoyalties(Long branchId, SettlementStatus status, String startMonth, String endMonth) {
        List<Royalty> royalties;

        if (branchId != null && status != null) {
            // 지점ID와 상태로 필터링
            royalties = royaltyRepository.findByBranchIdAndSettlementStatus(branchId, status);
        } else if (branchId != null) {
            // 지점ID로만 필터링
            royalties = royaltyRepository.findByBranchIdOrderByApplicableMonthDesc(branchId);
        } else if (status != null) {
            // 상태로만 필터링 - 전체 조회 후 필터링
            royalties = royaltyRepository.findAllWithBranch();
            royalties = royalties.stream()
                    .filter(r -> r.getSettlementStatus() == status)
                    .toList();
        } else {
            // 전체 조회
            royalties = royaltyRepository.findAllWithBranch();
        }

        // 연월 필터링
        if (startMonth != null && endMonth != null) {
            royalties = royalties.stream()
                    .filter(r -> {
                        String month = r.getApplicableMonth();
                        return month.compareTo(startMonth) >= 0 && month.compareTo(endMonth) <= 0;
                    })
                    .toList();
        }

        return royalties;
    }

    /**
     * 헤더 행 생성 (2행)
     */
    private void createHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(1);
        String[] headers = {
                "로열티번호", "지점명", "적용연월", "산정방법", "비율(%)", "고정금액(원)",
                "로열티금액(원)", "월매출액(원)", "납부기한", "정산상태", "실제납부일",
                "생성일시", "수정일시"
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
    private void createRoyaltyRow(Sheet sheet, int rowNum, Royalty royalty,
                                  CellStyle dataStyle, CellStyle numberStyle,
                                  CellStyle percentStyle, CellStyle dateStyle) {
        Row row = sheet.createRow(rowNum);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 로열티 기본 정보
        createCell(row, 0, "RY-" + String.format("%06d", royalty.getId()), dataStyle);  // 로열티번호
        createCell(row, 1, royalty.getBranch().getName(), dataStyle);  // 지점명
        createCell(row, 2, formatApplicableMonth(royalty.getApplicableMonth()), dataStyle);  // 적용연월
        createCell(row, 3, getCalculationMethodKorean(royalty.getCalculationMethod().name()), dataStyle);  // 산정방법

        // 비율 (백분율)
        if (royalty.getPercentage() != null) {
            createCell(row, 4, royalty.getPercentage(), percentStyle);  // 비율
        } else {
            createCell(row, 4, "-", dataStyle);
        }

        // 고정금액
        if (royalty.getFixedAmount() != null) {
            createCell(row, 5, royalty.getFixedAmount(), numberStyle);  // 고정금액
        } else {
            createCell(row, 5, "-", dataStyle);
        }

        createCell(row, 6, royalty.getAmount(), numberStyle);  // 로열티금액

        // 월매출액 조회
        Long totalSales = getTotalSales(royalty.getBranch().getId(), royalty.getApplicableMonth());
        createCell(row, 7, totalSales, numberStyle);  // 월매출액

        createCell(row, 8, royalty.getDueDate().format(dateFormatter), dateStyle);  // 납부기한
        createCell(row, 9, getSettlementStatusKorean(royalty.getSettlementStatus().name()), dataStyle);  // 정산상태

        // 실제납부일
        if (royalty.getPaymentDate() != null) {
            createCell(row, 10, royalty.getPaymentDate().format(dateTimeFormatter), dateStyle);
        } else {
            createCell(row, 10, "-", dataStyle);
        }

        createCell(row, 11, royalty.getCreatedAt().format(dateTimeFormatter), dateStyle);  // 생성일시
        createCell(row, 12, royalty.getUpdatedAt().format(dateTimeFormatter), dateStyle);  // 수정일시
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
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }

        cell.setCellStyle(style);
    }

    /**
     * 제목 행 생성 (1행) - 필터링 조건 포함
     */
    private void createTitleRow(Sheet sheet, CellStyle titleStyle, Long branchId,
                                SettlementStatus status, String startMonth, String endMonth) {
        Row titleRow = sheet.createRow(0);

        // 제목 텍스트 생성
        StringBuilder title = new StringBuilder("로열티 내역");

        if (branchId != null) {
            title.append(" (지점ID: ").append(branchId).append(")");
        }
        if (status != null) {
            title.append(" [정산상태: ").append(getSettlementStatusKorean(status.name())).append("]");
        }
        if (startMonth != null && endMonth != null) {
            title.append(" [기간: ").append(formatApplicableMonth(startMonth))
                 .append(" ~ ").append(formatApplicableMonth(endMonth)).append("]");
        }

        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title.toString());
        titleCell.setCellStyle(titleStyle);

        // 제목 셀 병합 (0행의 0~12열)
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 12));
    }

    /**
     * 제목 행 생성 (1행) - 특정 지점용
     */
    private void createBranchTitleRow(Sheet sheet, CellStyle titleStyle, Long branchId, String branchName) {
        Row titleRow = sheet.createRow(0);

        String title = branchName + " 로열티 내역 (지점ID: " + branchId + ")";

        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(titleStyle);

        // 제목 셀 병합 (0행의 0~12열)
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 12));
    }

    /**
     * 제목 스타일
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 배경색 (진한 회색)
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 테두리
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 가운데 정렬
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 폰트 (굵게, 크기 14)
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);

        return style;
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
     * 백분율 스타일
     */
    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 테두리
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 오른쪽 정렬
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 백분율 포맷
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00"));

        return style;
    }

    /**
     * 날짜 스타일
     */
    private CellStyle createDateStyle(Workbook workbook) {
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
     * 적용연월 포맷 변환 (202401 -> 2024-01)
     */
    private String formatApplicableMonth(String month) {
        if (month == null || month.length() != 6) {
            return month;
        }
        return month.substring(0, 4) + "-" + month.substring(4, 6);
    }

    /**
     * 산정방법 한글 변환
     */
    private String getCalculationMethodKorean(String method) {
        return switch (method) {
            case "PERCENTAGE" -> "매출액 비율";
            case "FIXED" -> "고정 금액";
            case "MIXED" -> "혼합형";
            default -> method;
        };
    }

    /**
     * 정산상태 한글 변환
     */
    private String getSettlementStatusKorean(String status) {
        return switch (status) {
            case "PENDING" -> "미납";
            case "PAID" -> "완납";
            case "OVERDUE" -> "연체";
            case "PARTIAL" -> "부분납부";
            default -> status;
        };
    }

    /**
     * 월매출액 조회 (Ordering 모듈 연동)
     */
    private Long getTotalSales(Long branchId, String applicableMonth) {
        try {
            OrderSalesResponseDto salesDto = orderClient.getBranchSales(branchId, applicableMonth);
            return salesDto.getTotalSales();
        } catch (Exception e) {
            log.warn("매출 정보 조회 실패 - branchId: {}, month: {}", branchId, applicableMonth, e);
            return 0L;
        }
    }
}

