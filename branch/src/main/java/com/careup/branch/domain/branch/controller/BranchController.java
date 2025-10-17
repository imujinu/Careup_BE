package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.branch.*;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/branch")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    // 지점 등록 API
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<?> register(
            @Valid @ModelAttribute BranchRegisterReqDto reqDto,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        try {
            Branch branch = branchService.registerBranch(reqDto, profileImage);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(branch.getId())
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 등록이 완료되었습니다.")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.CONFLICT.value())
                            .status_message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("지점 등록 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점 목록 조회 API (페이징)
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @GetMapping
    public ResponseEntity<?> getList(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            BranchListResDto branchList = branchService.getBranchList(pageable);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(branchList)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 목록 조회가 완료되었습니다.")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("지점 목록 조회 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점 상세 조회 API
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @GetMapping("/{branchId}")
    public ResponseEntity<?> getBranch(@PathVariable Long branchId) {
        try {
            BranchDto branchDetailDto = branchService.getBranch(branchId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(branchDetailDto)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 조회 성공")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.CONFLICT.value())
                            .status_message("지점 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점 수정 API
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @PatchMapping(value = "/{branchId}", consumes = "multipart/form-data")
    public ResponseEntity<?> update(
            @PathVariable Long branchId,
            @Validated @ModelAttribute BranchUpdateDto request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) {
        try {
            Branch updatedBranch = branchService.updateBranch(branchId, request, profileImage);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(updatedBranch)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 수정 완료")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("지점 수정 실패: " + e.getMessage())
            );
        }
    }

    // 지점 삭제 API
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @DeleteMapping("/{branchId}")
    public ResponseEntity<?> delete(@PathVariable Long branchId) {
        try {
            branchService.deleteBranch(branchId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(branchId)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 삭제 완료")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("지점 삭제 실패 : " + e.getMessage())
            );
        }
    }

    /**
     * Branch ID 목록으로 Branch 정보 조회 (ordering 서비스용)
     * GET /branch/list-by-ids?branchIds=1,2,3
     */
    @GetMapping("/list-by-ids")
    public ResponseEntity<?> getBranchesByIds(@RequestParam List<Long> branchIds) {
        try {
            List<BranchSimpleDto> branches = branchService.getBranchesByIds(branchIds);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(branches)
                            .status_code(HttpStatus.OK.value())
                            .status_message("Branch 정보 조회 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("Branch 정보 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 특정 지점의 인근 지점 조회 (위치 기반)
     * GET /branch/{branchId}/nearby?radiusKm=10
     */
    @GetMapping("/{branchId}/nearby")
    public ResponseEntity<?> getNearbyBranches(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {
        try {
            List<NearbyBranchDto> nearbyBranches = branchService.getNearbyBranches(branchId, radiusKm);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(nearbyBranches)
                            .status_code(HttpStatus.OK.value())
                            .status_message("인근 지점 조회 성공")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message(e.getMessage())
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("인근 지점 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }
}
