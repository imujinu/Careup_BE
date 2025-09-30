package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.BranchListResDto;
import com.careup.branch.domain.branch.dto.BranchRegisterReqDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/branch")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    // 지점 등록 API
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody BranchRegisterReqDto reqDto) {
        try {
            Branch branch = branchService.registerBranch(reqDto);
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
    @GetMapping("/{page}")
    public ResponseEntity<?> getList(@PathVariable int page) {
        try {
            BranchListResDto branchList = branchService.getBranchList(page);
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
}