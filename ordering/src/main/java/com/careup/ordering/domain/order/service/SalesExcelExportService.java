package com.careup.ordering.domain.order.service;

import com.careup.ordering.domain.order.dto.AllBranchesSalesDto;
import com.careup.ordering.domain.order.dto.BranchSalesDetailDto;
import com.careup.ordering.domain.order.dto.SalesForecastDto;
import com.careup.ordering.domain.order.dto.request.ExcelExportRequestDto;
import com.careup.ordering.domain.order.dto.request.HqSalesRequestDto;
import com.careup.ordering.domain.order.dto.response.AllBranchesSalesResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchComparisonResponseDto;
import com.careup.ordering.domain.order.dto.response.BranchSalesDetailResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 매출 리포트 엑셀 변환 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesExcelExportService {

    private final HqSalesService hqSalesService;
    private final SalesForecastIntegrationService salesForecastIntegrationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 엑셀 파일 생성 (메인 메서드)
     */
    public byte[] generateExcel(ExcelExportRequestDto request) throws IOException {
        log.info("엑셀 생성 시작 - Type: {}", request.getExportType());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            switch (request.getExportType().toUpperCase()) {
                case "ALL_BRANCHES":
                    createAllBranchesSalesExcel(workbook, request);
                    break;
                case "BRANCH_DETAIL":
                    createBranchDetailExcel(workbook, request);
                    break;
                case "BRANCH_COMPARISON":
                    createBranchComparisonExcel(workbook, request);
                    break;
                case "SALES_FORECAST":
                    createSalesForecastExcel(workbook, request);
                    break;
                default:
                    throw new IllegalArgumentException("지원하지 않는 엑셀 타입입니다: " + request.getExportType());
            }

            workbook.write(outputStream);
            log.info("엑셀 생성 완료 - Type: {}", request.getExportType());
            return outputStream.toByteArray();
        }
    }

    /**
     * 전체 지점 매출 엑셀 생성
     */
    private void createAllBranchesSalesExcel(Workbook workbook, ExcelExportRequestDto request) {
        HqSalesRequestDto hqRequest = HqSalesRequestDto.withDates(
                request.getStartDate(),
                request.getEndDate(),
                request.getPeriodType()
        );
        AllBranchesSalesResponseDto data = hqSalesService.getAllBranchesSales(hqRequest);

        Sheet sheet = workbook.createSheet("전체 지점 매출");

        // 스타일 생성
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // 제목
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("전체 지점 매출 리포트");
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        rowNum++;

        // 요약 정보
        Row summaryRow1 = sheet.createRow(rowNum++);
        summaryRow1.createCell(0).setCellValue("기간");
        summaryRow1.createCell(1).setCellValue(request.getStartDate().format(DATE_FORMATTER) + " ~ " +
                                               request.getEndDate().format(DATE_FORMATTER));

        Row summaryRow2 = sheet.createRow(rowNum++);
        summaryRow2.createCell(0).setCellValue("총 매출액");
        Cell totalSalesCell = summaryRow2.createCell(1);
        totalSalesCell.setCellValue(data.getTotalSales());
        totalSalesCell.setCellStyle(currencyStyle);

        Row summaryRow3 = sheet.createRow(rowNum++);
        summaryRow3.createCell(0).setCellValue("총 주문 건수");
        summaryRow3.createCell(1).setCellValue(data.getTotalOrders());

        Row summaryRow4 = sheet.createRow(rowNum++);
        summaryRow4.createCell(0).setCellValue("총 지점 수");
        summaryRow4.createCell(1).setCellValue(data.getTotalBranchCount());
        rowNum++;

        // 헤더
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"날짜", "기간 타입", "총 매출", "총 주문 수", "평균 주문 금액", "활성 지점 수", "지점당 평균 매출"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터
        for (AllBranchesSalesDto salesDto : data.getSalesData()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(salesDto.getDate().format(DATE_FORMATTER));
            row.createCell(1).setCellValue(salesDto.getPeriod());

            Cell salesCell = row.createCell(2);
            salesCell.setCellValue(salesDto.getTotalSales());
            salesCell.setCellStyle(currencyStyle);

            row.createCell(3).setCellValue(salesDto.getTotalOrders());

            Cell avgAmountCell = row.createCell(4);
            avgAmountCell.setCellValue(salesDto.getAverageOrderAmount());
            avgAmountCell.setCellStyle(currencyStyle);

            row.createCell(5).setCellValue(salesDto.getActiveBranchCount());

            Cell avgSalesPerBranchCell = row.createCell(6);
            avgSalesPerBranchCell.setCellValue(salesDto.getAverageSalesPerBranch());
            avgSalesPerBranchCell.setCellStyle(currencyStyle);
        }

        // 열 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
        }
    }

    /**
     * 지점 상세 매출 엑셀 생성
     */
    private void createBranchDetailExcel(Workbook workbook, ExcelExportRequestDto request) {
        HqSalesRequestDto hqRequest = HqSalesRequestDto.withDates(
                request.getStartDate(),
                request.getEndDate(),
                request.getPeriodType()
        );
        BranchSalesDetailResponseDto data = hqSalesService.getBranchSalesDetail(request.getBranchId(), hqRequest);

        Sheet sheet = workbook.createSheet("지점 상세 매출");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // 제목
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("지점 상세 매출 리포트");
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        rowNum++;

        // 요약 정보
        Row summaryRow1 = sheet.createRow(rowNum++);
        summaryRow1.createCell(0).setCellValue("지점명");
        summaryRow1.createCell(1).setCellValue(data.getBranchName());

        Row summaryRow2 = sheet.createRow(rowNum++);
        summaryRow2.createCell(0).setCellValue("기간");
        summaryRow2.createCell(1).setCellValue(request.getStartDate().format(DATE_FORMATTER) + " ~ " +
                                               request.getEndDate().format(DATE_FORMATTER));

        Row summaryRow3 = sheet.createRow(rowNum++);
        summaryRow3.createCell(0).setCellValue("총 매출액");
        Cell totalSalesCell = summaryRow3.createCell(1);
        totalSalesCell.setCellValue(data.getTotalSales());
        totalSalesCell.setCellStyle(currencyStyle);

        Row summaryRow4 = sheet.createRow(rowNum++);
        summaryRow4.createCell(0).setCellValue("총 주문 건수");
        summaryRow4.createCell(1).setCellValue(data.getTotalOrders());

        Row summaryRow5 = sheet.createRow(rowNum++);
        summaryRow5.createCell(0).setCellValue("시장 점유율");
        summaryRow5.createCell(1).setCellValue(String.format("%.2f%%", data.getMarketShare()));

        Row summaryRow6 = sheet.createRow(rowNum++);
        summaryRow6.createCell(0).setCellValue("순위");
        summaryRow6.createCell(1).setCellValue(data.getRanking());
        rowNum++;

        // 헤더
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"날짜", "기간 타입", "총 매출", "총 주문 수", "평균 주문 금액", "시장 점유율(%)", "순위"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터
        for (BranchSalesDetailDto salesDto : data.getSalesData()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(salesDto.getDate().format(DATE_FORMATTER));
            row.createCell(1).setCellValue(salesDto.getPeriod());

            Cell salesCell = row.createCell(2);
            salesCell.setCellValue(salesDto.getTotalSales());
            salesCell.setCellStyle(currencyStyle);

            row.createCell(3).setCellValue(salesDto.getTotalOrders());

            Cell avgAmountCell = row.createCell(4);
            avgAmountCell.setCellValue(salesDto.getAverageOrderAmount());
            avgAmountCell.setCellStyle(currencyStyle);

            row.createCell(5).setCellValue(String.format("%.2f", salesDto.getMarketShare()));
            row.createCell(6).setCellValue(salesDto.getRanking());
        }

        // 열 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
        }
    }

    /**
     * 지점 비교 매출 엑셀 생성
     */
    private void createBranchComparisonExcel(Workbook workbook, ExcelExportRequestDto request) {
        HqSalesRequestDto hqRequest = HqSalesRequestDto.withDatesAndBranchIds(
                request.getBranchIds(),
                request.getStartDate(),
                request.getEndDate(),
                request.getPeriodType()
        );
        BranchComparisonResponseDto data = hqSalesService.compareBranchesSales(hqRequest);

        Sheet sheet = workbook.createSheet("지점 비교");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // 제목
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("지점 간 매출 비교 리포트");
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        rowNum++;

        // 요약 정보
        Row summaryRow1 = sheet.createRow(rowNum++);
        summaryRow1.createCell(0).setCellValue("비교 지점 수");
        summaryRow1.createCell(1).setCellValue(data.getBranchIds().size());

        Row summaryRow2 = sheet.createRow(rowNum++);
        summaryRow2.createCell(0).setCellValue("기간");
        summaryRow2.createCell(1).setCellValue(request.getStartDate().format(DATE_FORMATTER) + " ~ " +
                                               request.getEndDate().format(DATE_FORMATTER));

        Row summaryRow3 = sheet.createRow(rowNum++);
        summaryRow3.createCell(0).setCellValue("총 매출액");
        Cell totalSalesCell = summaryRow3.createCell(1);
        totalSalesCell.setCellValue(data.getTotalSales());
        totalSalesCell.setCellStyle(currencyStyle);
        rowNum++;

        // 헤더
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"지점명", "날짜", "기간 타입", "총 매출", "총 주문 수", "평균 주문 금액", "시장 점유율(%)", "순위"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터
        for (BranchSalesDetailDto salesDto : data.getComparisonData()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(salesDto.getBranchName());
            row.createCell(1).setCellValue(salesDto.getDate().format(DATE_FORMATTER));
            row.createCell(2).setCellValue(salesDto.getPeriod());

            Cell salesCell = row.createCell(3);
            salesCell.setCellValue(salesDto.getTotalSales());
            salesCell.setCellStyle(currencyStyle);

            row.createCell(4).setCellValue(salesDto.getTotalOrders());

            Cell avgAmountCell = row.createCell(5);
            avgAmountCell.setCellValue(salesDto.getAverageOrderAmount());
            avgAmountCell.setCellStyle(currencyStyle);

            row.createCell(6).setCellValue(String.format("%.2f", salesDto.getMarketShare()));
            row.createCell(7).setCellValue(salesDto.getRanking());
        }

        // 열 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
        }
    }

    /**
     * 예상 매출액 엑셀 생성
     */
    private void createSalesForecastExcel(Workbook workbook, ExcelExportRequestDto request) {
        // 모든 지점의 예상 매출액 조회
        java.util.List<SalesForecastDto> forecasts = salesForecastIntegrationService.getAllBranchesSalesForecasts(
                request.getForecastDays() != null ? request.getForecastDays() : 30
        );

        Sheet sheet = workbook.createSheet("예상 매출액");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // 제목
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("전체 지점 예상 매출액 리포트");
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        rowNum++;

        // 요약 정보
        Row summaryRow1 = sheet.createRow(rowNum++);
        summaryRow1.createCell(0).setCellValue("예측 기준일");
        summaryRow1.createCell(1).setCellValue(LocalDate.now().format(DATE_FORMATTER));

        Row summaryRow2 = sheet.createRow(rowNum++);
        summaryRow2.createCell(0).setCellValue("예측 기간");
        summaryRow2.createCell(1).setCellValue((request.getForecastDays() != null ? request.getForecastDays() : 30) + "일");
        rowNum++;

        // 헤더
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"지점 ID", "지점명", "과거 30일 매출", "예상 매출액", "예측 기간(일)", "증감률(%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 데이터
        for (SalesForecastDto forecast : forecasts) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(forecast.getBranchId());
            row.createCell(1).setCellValue(forecast.getBranchName());

            Cell pastSalesCell = row.createCell(2);
            pastSalesCell.setCellValue(forecast.getPastSales());
            pastSalesCell.setCellStyle(currencyStyle);

            Cell forecastSalesCell = row.createCell(3);
            forecastSalesCell.setCellValue(forecast.getForecastedSales());
            forecastSalesCell.setCellStyle(currencyStyle);

            row.createCell(4).setCellValue(forecast.getForecastDays());

            // 증감률 계산
            double changeRate = 0.0;
            if (forecast.getPastSales() > 0) {
                changeRate = ((double) (forecast.getForecastedSales() - forecast.getPastSales()) / forecast.getPastSales()) * 100;
            }
            row.createCell(5).setCellValue(String.format("%.2f", changeRate));
        }

        // 열 너비 자동 조정
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1024);
        }
    }

    /**
     * 제목 스타일 생성
     */
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 헤더 스타일 생성
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 데이터 스타일 생성
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 통화 스타일 생성
     */
    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}

