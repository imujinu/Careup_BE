package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.royalty.RoyaltyDetailResponseDto;
import com.careup.branch.domain.branch.dto.royalty.RoyaltyListResponseDto;
import com.careup.branch.domain.branch.dto.royalty.SettlementHistoryResponseDto;
import com.careup.branch.domain.branch.entity.SettlementStatus;
import com.careup.branch.domain.branch.service.RoyaltyExcelService;
import com.careup.branch.domain.branch.service.RoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/royalties")
@RequiredArgsConstructor
public class RoyaltyController {

    private final RoyaltyService royaltyService;
    private final RoyaltyExcelService royaltyExcelService;

    /**
     * 지점 전체 로열티 조회
     * GET /royalties
     */
    @GetMapping
    public ResponseEntity<?> getAllRoyalties() {
        try {
            List<RoyaltyListResponseDto> royalties = royaltyService.getAllRoyalties();
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(royalties)
                            .status_code(HttpStatus.OK.value())
                            .status_message("로열티 목록 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("로열티 목록 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 선택한 가맹점에 대한 로열티 상세 조회
     * GET /royalties/{royaltyId}
     */
    @GetMapping("/{royaltyId}")
    public ResponseEntity<?> getRoyaltyDetail(@PathVariable Long royaltyId) {
        try {
            RoyaltyDetailResponseDto royalty = royaltyService.getRoyaltyDetail(royaltyId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(royalty)
                            .status_code(HttpStatus.OK.value())
                            .status_message("선택한 가맹점 로열티 상세 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("선택한 가맹점 로열티 상세 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 선택한 가맹점의 정산 내역 조회 (전체)
     * GET /royalties/branches/{branchId}/settlements
     */
    @GetMapping("/branches/{branchId}/settlements")
    public ResponseEntity<?> getSettlementHistory(
            @PathVariable Long branchId) {
        try {
            List<SettlementHistoryResponseDto> history = royaltyService.getSettlementHistory(branchId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(history)
                            .status_code(HttpStatus.OK.value())
                            .status_message("선택한 가맹점 정산 내역 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("선택한 가맹점 정산 내역 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 선택한 가맹점의 정산 내역 조회 (정산 상태별 필터링)
     * GET /royalties/branches/{branchId}/settlements?status={status}
     */
    @GetMapping("/branches/{branchId}/settlements/filter")
    public ResponseEntity<?> getSettlementHistoryByStatus(
            @PathVariable Long branchId,
            @RequestParam SettlementStatus status) {
        try {
            List<SettlementHistoryResponseDto> history = royaltyService.getSettlementHistoryByStatus(branchId, status);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(history)
                            .status_code(HttpStatus.OK.value())
                            .status_message("선택한 가맹점 정산 내역 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("선택한 가맹점 정산 내역 조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 로열티 내역 엑셀 다운로드 (전체 또는 필터링)
     * GET /royalties/export/excel
     * @param branchId 지점 ID (선택, null이면 전체)
     * @param status 정산 상태 (선택, null이면 전체)
     * @param startMonth 시작 연월 (yyyyMM 형식, 선택)
     * @param endMonth 종료 연월 (yyyyMM 형식, 선택)
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        try {
            byte[] excelBytes = royaltyExcelService.exportToExcel(branchId, status, startMonth, endMonth);

            // 파일명 생성 (현재 시각 포함)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "로열티내역_" + timestamp + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage()).getBytes());
        }
    }

    /**
     * 특정 지점 로열티 내역 엑셀 다운로드
     * GET /royalties/branches/{branchId}/export/excel
     */
    @GetMapping("/branches/{branchId}/export/excel")
    public ResponseEntity<byte[]> exportBranchRoyaltyToExcel(@PathVariable Long branchId) {
        try {
            byte[] excelBytes = royaltyExcelService.exportBranchRoyaltyToExcel(branchId);

            // 파일명 생성 (현재 시각 포함)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "지점별_로열티내역_" + branchId + "_" + timestamp + ".xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.setContentLength(excelBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage().getBytes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("엑셀 파일 생성 중 오류가 발생했습니다: " + e.getMessage()).getBytes());
        }
    }
}
