package com.careup.ordering.domain.order.controller;

import com.careup.ordering.common.dto.CommonErrorDto;
import com.careup.ordering.domain.order.dto.request.ExcelExportRequestDto;
import com.careup.ordering.domain.order.service.SalesExcelExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 매출 리포트 엑셀 내보내기 API
 */
@Slf4j
@RestController
@RequestMapping("/hq/sales/excel")
@RequiredArgsConstructor
public class SalesExcelExportController {

    private final SalesExcelExportService salesExcelExportService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 전체 지점 매출 엑셀 내보내기
     * GET /hq/sales/excel/all-branches?startDate=2025-01-01&endDate=2025-01-31&periodType=DAY
     */
    @GetMapping("/all-branches")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> exportAllBranchesSales(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType) {

        try {
            log.info("전체 지점 매출 엑셀 내보내기 요청 - startDate: {}, endDate: {}, periodType: {}",
                    startDate, endDate, periodType);

            ExcelExportRequestDto request = ExcelExportRequestDto.builder()
                    .exportType("ALL_BRANCHES")
                    .startDate(startDate)
                    .endDate(endDate)
                    .periodType(periodType)
                    .build();

            byte[] excelData = salesExcelExportService.generateExcel(request);

            String fileName = String.format("전체지점매출_%s_%s.xlsx",
                    startDate.format(DATE_FORMATTER),
                    endDate.format(DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));
            headers.setContentLength(excelData.length);

            log.info("전체 지점 매출 엑셀 내보내기 완료 - fileName: {}", fileName);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("전체 지점 매출 엑셀 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("전체 지점 매출 엑셀 내보내기 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("엑셀 내보내기 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 지점 상세 매출 엑셀 내보내기
     * GET /hq/sales/excel/branch/{branchId}?startDate=2025-01-01&endDate=2025-01-31&periodType=DAY
     */
    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> exportBranchDetailSales(
            @PathVariable Long branchId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType) {

        try {
            log.info("지점 상세 매출 엑셀 내보내기 요청 - branchId: {}, startDate: {}, endDate: {}, periodType: {}",
                    branchId, startDate, endDate, periodType);

            ExcelExportRequestDto request = ExcelExportRequestDto.builder()
                    .exportType("BRANCH_DETAIL")
                    .branchId(branchId)
                    .startDate(startDate)
                    .endDate(endDate)
                    .periodType(periodType)
                    .build();

            byte[] excelData = salesExcelExportService.generateExcel(request);

            String fileName = String.format("지점상세매출_%d_%s_%s.xlsx",
                    branchId,
                    startDate.format(DATE_FORMATTER),
                    endDate.format(DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));
            headers.setContentLength(excelData.length);

            log.info("지점 상세 매출 엑셀 내보내기 완료 - fileName: {}", fileName);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("지점 상세 매출 엑셀 생성 실패 - branchId: {}", branchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("지점 상세 매출 엑셀 내보내기 실패 - branchId: {}", branchId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("엑셀 내보내기 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 지점 비교 매출 엑셀 내보내기
     * GET /hq/sales/excel/comparison?branchIds=1,2,3&startDate=2025-01-01&endDate=2025-01-31&periodType=DAY
     */
    @GetMapping("/comparison")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> exportBranchComparisonSales(
            @RequestParam List<Long> branchIds,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "DAY") String periodType) {

        try {
            log.info("지점 비교 매출 엑셀 내보내기 요청 - branchIds: {}, startDate: {}, endDate: {}, periodType: {}",
                    branchIds, startDate, endDate, periodType);

            ExcelExportRequestDto request = ExcelExportRequestDto.builder()
                    .exportType("BRANCH_COMPARISON")
                    .branchIds(branchIds)
                    .startDate(startDate)
                    .endDate(endDate)
                    .periodType(periodType)
                    .build();

            byte[] excelData = salesExcelExportService.generateExcel(request);

            String fileName = String.format("지점비교매출_%s_%s.xlsx",
                    startDate.format(DATE_FORMATTER),
                    endDate.format(DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));
            headers.setContentLength(excelData.length);

            log.info("지점 비교 매출 엑셀 내보내기 완료 - fileName: {}", fileName);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("지점 비교 매출 엑셀 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("지점 비교 매출 엑셀 내보내기 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("엑셀 내보내기 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 예상 매출액 엑셀 내보내기
     * GET /hq/sales/excel/forecast?forecastDays=30
     */
    @GetMapping("/forecast")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> exportSalesForecast(
            @RequestParam(defaultValue = "30") Integer forecastDays) {

        try {
            log.info("예상 매출액 엑셀 내보내기 요청 - forecastDays: {}", forecastDays);

            ExcelExportRequestDto request = ExcelExportRequestDto.builder()
                    .exportType("SALES_FORECAST")
                    .forecastDays(forecastDays)
                    .build();

            byte[] excelData = salesExcelExportService.generateExcel(request);

            String fileName = String.format("예상매출액_%s.xlsx",
                    LocalDate.now().format(DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));
            headers.setContentLength(excelData.length);

            log.info("예상 매출액 엑셀 내보내기 완료 - fileName: {}", fileName);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("예상 매출액 엑셀 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("예상 매출액 엑셀 내보내기 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("엑셀 내보내기 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 통합 엑셀 내보내기 (POST)
     * POST /hq/sales/excel/export
     * Body: ExcelExportRequestDto
     */
    @PostMapping("/export")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<?> exportSalesExcel(@RequestBody ExcelExportRequestDto request) {

        try {
            log.info("통합 엑셀 내보내기 요청 - exportType: {}", request.getExportType());

            byte[] excelData = salesExcelExportService.generateExcel(request);

            String fileName = generateFileName(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));
            headers.setContentLength(excelData.length);

            log.info("통합 엑셀 내보내기 완료 - fileName: {}", fileName);

            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("엑셀 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            log.error("엑셀 내보내기 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("엑셀 내보내기 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 파일명 생성 헬퍼 메서드
     */
    private String generateFileName(ExcelExportRequestDto request) {
        String baseFileName;
        String dateStr = LocalDate.now().format(DATE_FORMATTER);

        switch (request.getExportType().toUpperCase()) {
            case "ALL_BRANCHES":
                baseFileName = String.format("전체지점매출_%s_%s",
                        request.getStartDate().format(DATE_FORMATTER),
                        request.getEndDate().format(DATE_FORMATTER));
                break;
            case "BRANCH_DETAIL":
                baseFileName = String.format("지점상세매출_%d_%s_%s",
                        request.getBranchId(),
                        request.getStartDate().format(DATE_FORMATTER),
                        request.getEndDate().format(DATE_FORMATTER));
                break;
            case "BRANCH_COMPARISON":
                baseFileName = String.format("지점비교매출_%s_%s",
                        request.getStartDate().format(DATE_FORMATTER),
                        request.getEndDate().format(DATE_FORMATTER));
                break;
            case "SALES_FORECAST":
                baseFileName = String.format("예상매출액_%s", dateStr);
                break;
            default:
                baseFileName = String.format("매출리포트_%s", dateStr);
        }

        return baseFileName + ".xlsx";
    }
}

