package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.ScheduleEventUpdateDto;
import com.careup.branch.domain.employee.dto.response.ScheduleEventDetailDto;
import com.careup.branch.domain.employee.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/event/{scheduleId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> eventDetail(@PathVariable Long scheduleId) {
        ScheduleEventDetailDto result = attendanceService.detail(scheduleId);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("근태 이벤트 상세 조회 완료")
                .build());
    }

    @PatchMapping("/event/{scheduleId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> upsertEvent(@PathVariable Long scheduleId,
                                                        @Valid @RequestBody ScheduleEventUpdateDto req) {
        ScheduleEventDetailDto result = attendanceService.upsertEvent(scheduleId, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("근태 이벤트 저장 완료")
                .build());
    }

    @DeleteMapping("/event/{scheduleId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> deleteEvent(@PathVariable Long scheduleId) {
        attendanceService.deleteEvent(scheduleId);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result("ok")
                .status_code(HttpStatus.OK.value())
                .status_message("근태 이벤트 삭제 완료")
                .build());
    }
}
