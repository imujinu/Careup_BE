package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.branch.BranchDto;
import com.careup.branch.domain.branch.dto.branch.BranchSelfUpdateDto;
import com.careup.branch.domain.branch.service.BranchSelfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/branch/self")
@RequiredArgsConstructor
public class BranchSelfController {

    private final BranchSelfService branchSelfService;

    // 가맹점주 - 내 가맹점 목록
    @PreAuthorize("hasRole('FRANCHISE_OWNER')")
    @GetMapping("/franchise")
    public ResponseEntity<?> listMyFranchise() {
        try {
            List<BranchDto> branches = branchSelfService.listMyFranchiseBranches();
            return ResponseEntity.ok(CommonSuccessDto.builder()
                    .result(branches)
                    .status_code(HttpStatus.OK.value())
                    .status_message("가맹점 목록 조회 성공")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("가맹점 목록 조회 실패: " + e.getMessage())
                            .build());
        }
    }

    // 가맹점주 - 지점 상세
    @PreAuthorize("hasRole('FRANCHISE_OWNER')")
    @GetMapping("/franchise/{branchId}")
    public ResponseEntity<?> getMyFranchise(@PathVariable Long branchId) {
        try {
            BranchDto dto = branchSelfService.getFranchiseBranch(branchId);
            return ResponseEntity.ok(CommonSuccessDto.builder()
                    .result(dto)
                    .status_code(HttpStatus.OK.value())
                    .status_message("가맹점 상세 조회 성공")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("가맹점 상세 조회 실패: " + e.getMessage())
                            .build());
        }
    }

    // 가맹점주 - 수정 승인요청 (name, profileImageUrl)
    @PreAuthorize("hasRole('FRANCHISE_OWNER')")
    @PatchMapping("/franchise/{branchId}")
    public ResponseEntity<?> requestFranchiseUpdate(@PathVariable Long branchId,
                                                    @Valid @RequestBody BranchSelfUpdateDto request) {
        try {
            String note = branchSelfService.requestFranchiseSelfUpdate(branchId, request);
            return ResponseEntity.ok(CommonSuccessDto.builder()
                    .result(note)
                    .status_code(HttpStatus.OK.value())
                    .status_message("수정 승인요청 등록 완료")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("수정 승인요청 실패: " + e.getMessage())
                            .build());
        }
    }

    // 직영점주 - 내 직영점 목록
    @PreAuthorize("hasRole('BRANCH_ADMIN')")
    @GetMapping("/direct")
    public ResponseEntity<?> listMyDirect() {
        try {
            List<BranchDto> branches = branchSelfService.listMyDirectBranches();
            return ResponseEntity.ok(CommonSuccessDto.builder()
                    .result(branches)
                    .status_code(HttpStatus.OK.value())
                    .status_message("직영점 목록 조회 성공")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("직영점 목록 조회 실패: " + e.getMessage())
                            .build());
        }
    }

    // 직영점주 - 지점 상세
    @PreAuthorize("hasRole('BRANCH_ADMIN')")
    @GetMapping("/direct/{branchId}")
    public ResponseEntity<?> getMyDirect(@PathVariable Long branchId) {
        try {
            BranchDto dto = branchSelfService.getDirectBranch(branchId);
            return ResponseEntity.ok(CommonSuccessDto.builder()
                    .result(dto)
                    .status_code(HttpStatus.OK.value())
                    .status_message("직영점 상세 조회 성공")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("직영점 상세 조회 실패: " + e.getMessage())
                            .build());
        }
    }

    // 직영점주 - 수정 승인요청
    @PreAuthorize("hasRole('BRANCH_ADMIN')")
    @PatchMapping("/direct/{branchId}")
    public ResponseEntity<?> requestDirectUpdate(@PathVariable Long branchId,
                                                 @Valid @RequestBody BranchSelfUpdateDto request) {
        try {
            String note = branchSelfService.requestDirectSelfUpdate(branchId, request);
            return ResponseEntity.ok(CommonSuccessDto.builder()
                    .result(note)
                    .status_code(HttpStatus.OK.value())
                    .status_message("수정 승인요청 등록 완료")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("수정 승인요청 실패: " + e.getMessage())
                            .build());
        }
    }
}

