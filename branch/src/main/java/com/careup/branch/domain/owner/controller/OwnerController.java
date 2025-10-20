package com.careup.branch.domain.owner.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.owner.dto.request.OwnerAssignRequestDto;
import com.careup.branch.domain.owner.dto.request.OwnerUpdateRequestDto;
import com.careup.branch.domain.owner.dto.response.OwnerResponseDto;
import com.careup.branch.domain.owner.service.OwnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    /**
     * 점주 등록 및 할당 (직원 -> 지점 관리자로 권한 승격)
     */
    @PostMapping("/assign")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> assignOwner(@Valid @RequestBody OwnerAssignRequestDto request) {
        OwnerResponseDto result = ownerService.assignOwner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("점주 등록 및 할당 완료")
                        .build()
        );
    }

    /**
     * 점주 정보 수정
     */
    @PatchMapping("/update/{employeeId}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> updateOwner(
            @PathVariable Long employeeId,
            @Valid @RequestBody OwnerUpdateRequestDto request) {
        OwnerResponseDto result = ownerService.updateOwner(employeeId, request);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("점주 정보 수정 완료")
                        .build()
        );
    }

    /**
     * 점주 삭제 (권한 해제)
     */
    @DeleteMapping("/remove/{employeeId}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> removeOwner(@PathVariable Long employeeId) {
        ownerService.removeOwner(employeeId);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(HttpStatus.OK.value())
                        .status_message("점주 삭제(권한 해제) 완료")
                        .build()
        );
    }

    /**
     * 특정 지점의 점주 조회
     */
    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> getOwnerByBranch(@PathVariable Long branchId) {
        OwnerResponseDto result = ownerService.getOwnerByBranch(branchId);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("지점 점주 조회 완료")
                        .build()
        );
    }

    /**
     * 모든 점주 목록 조회
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('HQ_ADMIN')")
    public ResponseEntity<CommonSuccessDto> getAllOwners() {
        List<OwnerResponseDto> result = ownerService.getAllOwners();
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("전체 점주 목록 조회 완료")
                        .build()
        );
    }
}

