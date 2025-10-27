package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.branch.BranchDto;
import com.careup.branch.domain.branch.dto.branch.BranchUpdateRequestDto;
import com.careup.branch.domain.branch.service.BranchManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/branch/my-branch")
@RequiredArgsConstructor
public class BranchManagerController {

    private final BranchManagerService branchManagerService;

    // 직영점장/가맹점장 자신의 지점 정보 조회
    @PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'FRANCHISE_ADMIN')")
    @GetMapping()
    public ResponseEntity<?> getMyBranch() {
        try {
            BranchDto myBranch = branchManagerService.getMyBranch();
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
    @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> requestBranchUpdate(
            @RequestPart("data") BranchUpdateRequestDto requestDto,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImageFile
            ) {
        try {
            branchManagerService.requestBranchUpdate(requestDto, profileImageFile);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result("수정 요청이 정상적으로 처리되었습니다.")
                            .status_code(HttpStatus.OK.value())
                            .status_message("수정 요청 성공")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .status_message("수정 요청 실패 error: " + e.getMessage())
                            .build()
            );
        }
    }
}
