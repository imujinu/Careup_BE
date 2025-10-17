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
    public ResponseEntity<CommonSuccessDto> eventDetail(@PathVariable Long scheduleId) {
        ScheduleEventDetailDto result = attendanceService.detail(scheduleId);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("근태 이벤트 상세 조회 완료")
                .build());
    }

    /** 관리/보정: 관리자만 가능 */
    @PatchMapping("/event/{scheduleId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> upsertEvent(@PathVariable Long scheduleId,
                                                        @Valid @RequestBody ScheduleEventUpdateDto req) {
        ScheduleEventDetailDto result = attendanceService.upsertEvent(scheduleId, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("근태 이벤트 저장 완료")
                .build());
    }

    /** 삭제: 관리자만 가능 */
    @DeleteMapping("/event/{scheduleId}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> deleteEvent(@PathVariable Long scheduleId) {
        attendanceService.deleteEvent(scheduleId);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result("ok")
                .status_code(HttpStatus.OK.value())
                .status_message("근태 이벤트 삭제 완료")
                .build());
    }

    // 액션 엔드포인트 (서버 now + 지오펜스 검증) — STAFF 포함 허용
    @PostMapping("/event/{scheduleId}/clock-in")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> clockIn(@PathVariable Long scheduleId,
                                                    @RequestBody(required = false) AttendanceActionRequest req) {
        ScheduleEventDetailDto result = attendanceService.clockIn(scheduleId, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("출근 처리 완료")
                .build());
    }

    @PostMapping("/event/{scheduleId}/break-start")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> breakStart(@PathVariable Long scheduleId,
                                                       @RequestBody(required = false) AttendanceActionRequest req) {
        ScheduleEventDetailDto result = attendanceService.breakStart(scheduleId, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("휴게 시작 처리 완료")
                .build());
    }

    @PostMapping("/event/{scheduleId}/break-end")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> breakEnd(@PathVariable Long scheduleId,
                                                     @RequestBody(required = false) AttendanceActionRequest req) {
        ScheduleEventDetailDto result = attendanceService.breakEnd(scheduleId, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("휴게 종료 처리 완료")
                .build());
    }

    @PostMapping("/event/{scheduleId}/clock-out")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> clockOut(@PathVariable Long scheduleId,
                                                     @RequestBody(required = false) AttendanceActionRequest req) {
        ScheduleEventDetailDto result = attendanceService.clockOut(scheduleId, req);
        return ResponseEntity.ok(CommonSuccessDto.builder()
                .result(result)
                .status_code(HttpStatus.OK.value())
                .status_message("퇴근 처리 완료")
                .build());
    }
}
