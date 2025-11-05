package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.AttendanceActionRequest;
import com.careup.branch.domain.employee.dto.request.ScheduleEventUpdateDto;
import com.careup.branch.domain.employee.dto.response.ScheduleEventDetailDto;
import com.careup.branch.domain.employee.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/event/{scheduleId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> eventDetail(@PathVariable String scheduleId) {
        Long id = parseScheduleId(scheduleId);
        ScheduleEventDetailDto result = attendanceService.detail(id);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("근태 이벤트 상세 조회 완료")
                .build());
    }

    @PatchMapping("/event/{scheduleId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> upsertEvent(@PathVariable String scheduleId,
                                                        @Valid @RequestBody ScheduleEventUpdateDto req) {
        Long id = parseScheduleId(scheduleId);
        ScheduleEventDetailDto result = attendanceService.upsertEvent(id, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("근태 이벤트 저장 완료")
                .build());
    }

    @DeleteMapping("/event/{scheduleId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> deleteEvent(@PathVariable String scheduleId) {
        Long id = parseScheduleId(scheduleId);
        attendanceService.deleteEvent(id);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result("ok")
                .status_code(HttpStatus.OK.value())
                .status_message("근태 이벤트 삭제 완료")
                .build());
    }

    @PostMapping("/event/{scheduleId}/clock-in")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> clockIn(@PathVariable String scheduleId,
                                                    @RequestBody(required = false) AttendanceActionRequest req) {
        Long id = parseScheduleId(scheduleId);
        ScheduleEventDetailDto result = attendanceService.clockIn(id, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("출근 처리 완료")
                .build());
    }

    @PostMapping("/event/{scheduleId}/break-start")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> breakStart(@PathVariable String scheduleId,
                                                       @RequestBody(required = false) AttendanceActionRequest req) {
        Long id = parseScheduleId(scheduleId);
        ScheduleEventDetailDto result = attendanceService.breakStart(id, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("휴게 시작 처리 완료")
                .build());
    }

    @PostMapping("/event/{scheduleId}/break-end")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> breakEnd(@PathVariable String scheduleId,
                                                     @RequestBody(required = false) AttendanceActionRequest req) {
        Long id = parseScheduleId(scheduleId);
        ScheduleEventDetailDto result = attendanceService.breakEnd(id, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("휴게 종료 처리 완료")
                .build());
    }

    @PostMapping("/event/{scheduleId}/clock-out")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> clockOut(@PathVariable String scheduleId,
                                                     @RequestBody(required = false) AttendanceActionRequest req) {
        Long id = parseScheduleId(scheduleId);
        ScheduleEventDetailDto result = attendanceService.clockOut(id, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("퇴근 처리 완료")
                .build());
    }

    private Long parseScheduleId(String raw) {
        if (raw == null) throw new IllegalArgumentException("invalid scheduleId");
        int idx = raw.indexOf(':');
        String head = idx >= 0 ? raw.substring(0, idx) : raw;
        return Long.valueOf(head);
    }
}
