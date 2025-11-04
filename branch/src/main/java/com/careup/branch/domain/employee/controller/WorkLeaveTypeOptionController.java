// src/main/java/com/careup/branch/domain/employee/controller/WorkLeaveTypeOptionController.java
package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.response.LeaveTypeOptionDto;
import com.careup.branch.domain.employee.dto.response.WorkTypeOptionDto;
import com.careup.branch.domain.employee.entity.LeaveType;
import com.careup.branch.domain.employee.entity.WorkType;
import com.careup.branch.domain.employee.repository.LeaveTypeRepository;
import com.careup.branch.domain.employee.repository.WorkTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class WorkLeaveTypeOptionController {

    private final WorkTypeRepository workTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;

    // 프론트: GET /branch-service/api/work-types/options
    @GetMapping("/work-types/options")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> workTypeOptions() {
        List<WorkTypeOptionDto> result = workTypeRepository
                .findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream().map(WorkTypeOptionDto::fromEntity).toList();

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("근무 종류 옵션 조회 완료")
                        .build()
        );
    }

    // 프론트: GET /branch-service/api/leave-types/options
    @GetMapping("/leave-types/options")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> leaveTypeOptions() {
        List<LeaveTypeOptionDto> result = leaveTypeRepository
                .findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream().map(LeaveTypeOptionDto::fromEntity).toList();

        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("휴가 종류 옵션 조회 완료")
                        .build()
        );
    }
}
