package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.branch.BranchDto;
import com.careup.branch.domain.branch.service.BranchManagerService;
import com.careup.branch.domain.employee.entity.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/manager/branch")
@RequiredArgsConstructor
public class BranchManagerController {

    private final BranchManagerService branchManagerService;

    // 직영점장/가맹점장 자신의 지점 정보 조회
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'FRANCHISE_ADMIN')")
    @GetMapping("/my-branch")
    public ResponseEntity<?> getMyBranch(@AuthenticationPrincipal Employee employee) {
        // Spring Security에서 인증된 사용자(직원 ID) 정보 가져오기
        try {
            Long employeeId = employee.getId();
            BranchDto myBranch = branchManagerService.getMyBranch(employeeId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(myBranch)
                            .status_code(HttpStatus.OK.value())
                            .status_message("조회 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("조회 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 직영점장/가맹점장이 자신의 지점 정보 수정 요청
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'FRANCHISE_ADMIN')")
    @PutMapping(value = "/my-branch", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> requestBranchUpdate(
            @AuthenticationPrincipal Employee employee,
            @RequestParam("name") String name,
            @RequestPart(value = "profileImage", required = false)MultipartFile profileImageFile
            ) {
        try {
            Long employeeId = employee.getId();
            branchManagerService.requestBranchUpdate(employeeId, name, profileImageFile);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(employeeId)
                            .status_code(HttpStatus.OK.value())
                            .status_message("수정 요청 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("수정 요창 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }
}
