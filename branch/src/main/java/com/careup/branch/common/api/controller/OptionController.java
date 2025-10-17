package com.careup.branch.common.api.controller;

import com.careup.branch.common.api.dto.response.BranchOptionDto;
import com.careup.branch.common.api.dto.response.EmployeeOptionDto;
import com.careup.branch.common.api.service.OptionQueryService;
import com.careup.branch.common.dto.CommonSuccessDto;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OptionController {

    private final OptionQueryService optionQueryService;

    @GetMapping("/branches/options")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> branchOptions(@RequestParam(required = false) String keyword) {
        List<BranchOptionDto> result = optionQueryService.branchOptions(keyword);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("지점 옵션 조회 완료")
                        .build()
        );
    }

    @GetMapping("/employees/options")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> employeeOptions(
            @RequestParam(required = false) List<Long> branchIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "all", defaultValue = "false") boolean all
    ) {
        List<EmployeeOptionDto> result = optionQueryService.employeeOptions(branchIds, from, to, keyword, all);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message(all ? "직원 옵션(전사) 조회 완료" : "직원 옵션 조회 완료")
                        .build()
        );
    }
}
