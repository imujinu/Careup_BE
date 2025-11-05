package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.response.EmployeeDetailDto;
import com.careup.branch.domain.employee.dto.response.EmployeeListDto;
import com.careup.branch.domain.employee.service.EmployeeQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/employees")
public class EmployeeQueryController {

    private final EmployeeQueryService employeeQueryService;

    // 목록 조회: 본점/지점/가맹 관리자만
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<EmployeeDetailDto> page = employeeQueryService.list(pageable);
        EmployeeListDto result = EmployeeListDto.fromPage(page);

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("직원 목록 조회")
                        .build()
        );
    }

    // 지점별 소속 직원 목록 조회: 본점/지점/가맹 관리자만
    @GetMapping("/list/branch/{branchId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> listByBranch(
            @PathVariable Long branchId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String employmentStatus,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) String authorityType,
            @PageableDefault(size = 20, sort = "employmentStatus,asc") Pageable pageable) {
        Page<EmployeeDetailDto> page = employeeQueryService.listByBranch(
                branchId, search, employmentStatus, gender, employmentType, authorityType, pageable);
        EmployeeListDto result = EmployeeListDto.fromPage(page);
        log.info(result.toString());

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("지점별 직원 목록 조회")
                        .build()
        );
    }

    // 상세 조회: 인증자 누구나 (서비스에서 권한 보정)
    @GetMapping("/detail/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonSuccessDto> detail(@PathVariable Long id) {
        EmployeeDetailDto result = employeeQueryService.getDetail(id);

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("직원 상세 조회")
                        .build()
        );
    }

    // 마이페이지: 현재 로그인한 직원 본인 정보
    @GetMapping("/my-page")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonSuccessDto> myPage() {
        EmployeeDetailDto result = employeeQueryService.getMyPage();

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("마이페이지 조회")
                        .build()
        );
    }

    // 알림용 전체 직원 조회
    @GetMapping("/chat/list")
    public ResponseEntity<CommonSuccessDto> employeesList(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<EmployeeDetailDto> page = employeeQueryService.list(pageable);
        EmployeeListDto result = EmployeeListDto.fromPage(page);

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("직원 목록 조회")
                        .build()
        );
    }

    // 내부 API용: employeeId로 현재 활성화된 branchId 조회
    @GetMapping("/internal/branch-id/{employeeId}")
    public ResponseEntity<CommonSuccessDto> getBranchIdByEmployeeId(@PathVariable Long employeeId) {
        Long branchId = employeeQueryService.getBranchIdByEmployeeId(employeeId);
        
        if (branchId == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonSuccessDto.builder()
                            .result(null)
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("활성화된 지점 배치 정보를 찾을 수 없습니다.")
                            .build()
            );
        }
        
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(Map.of("branchId", branchId))
                        .status_code(HttpStatus.OK.value())
                        .status_message("지점 ID 조회 성공")
                        .build()
        );
    }
}
