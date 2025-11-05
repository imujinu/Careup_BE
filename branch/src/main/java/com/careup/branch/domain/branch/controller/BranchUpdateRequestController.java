package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.branch.BranchUpdateRequestDto;
import com.careup.branch.domain.branch.dto.branch.BranchUpdateRequestListResDto;
import com.careup.branch.domain.branch.entity.BranchUpdateRequest;
import com.careup.branch.domain.branch.service.BranchUpdateRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/branch/update-requests")
@RequiredArgsConstructor
public class BranchUpdateRequestController {

    private final BranchUpdateRequestService branchUpdateRequestService;

    /**
     * 모든 지점 수정 요청 목록 조회 (본사 관리자)
     */
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @GetMapping
    public ResponseEntity<?> getAllRequests(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            BranchUpdateRequestListResDto requests = branchUpdateRequestService.getAllRequests(pageable);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(requests)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 수정 요청 목록 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("지점 수정 요청 목록 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 특정 상태의 지점 수정 요청 목록 조회 (본사 관리자)
     */
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getRequestsByStatus(
            @PathVariable BranchUpdateRequest.RequestStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            BranchUpdateRequestListResDto requests = branchUpdateRequestService.getRequestsByStatus(status, pageable);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(requests)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 수정 요청 목록 조회 완료 (상태: " + status + ")")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("지점 수정 요청 목록 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 지점 수정 요청 상세 조회 (본사 관리자)
     */
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @GetMapping("/{requestId}")
    public ResponseEntity<?> getRequest(@PathVariable Long requestId) {
        try {
            BranchUpdateRequestDto request = branchUpdateRequestService.getRequest(requestId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(request)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 수정 요청 상세 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("지점 수정 요청 상세 조회 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 지점 수정 요청 승인 (본사 관리자)
     */
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<?> approveRequest(@PathVariable Long requestId) {
        try {
            BranchUpdateRequestDto updatedRequest = branchUpdateRequestService.approveRequest(requestId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(updatedRequest)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 수정 요청이 승인되었습니다.")
                            .build()
            );
        } catch (IllegalStateException e) {
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
                            .status_message("지점 수정 요청 승인 실패: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 지점 수정 요청 거부 (본사 관리자)
     */
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long requestId) {
        try {
            BranchUpdateRequestDto updatedRequest = branchUpdateRequestService.rejectRequest(requestId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(updatedRequest)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 수정 요청이 거부되었습니다.")
                            .build()
            );
        } catch (IllegalStateException e) {
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
                            .status_message("지점 수정 요청 거부 실패: " + e.getMessage())
                            .build()
            );
        }
    }
}

