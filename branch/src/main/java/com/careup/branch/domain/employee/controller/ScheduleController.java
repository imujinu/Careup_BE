package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.ScheduleCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleUpdateDto;
import com.careup.branch.domain.employee.dto.response.ScheduleCalendarDto;
import com.careup.branch.domain.employee.dto.response.ScheduleDetailDto;
import com.careup.branch.domain.employee.dto.response.ScheduleListDto;
import com.careup.branch.domain.employee.service.ScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
@Validated
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> create(@Valid @RequestBody ScheduleCreateDto req) {
        ScheduleDetailDto result = scheduleService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("스케줄 생성 완료")
                        .build()
        );
    }

    @PostMapping("/mass-create")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> massCreate(@Valid @RequestBody ScheduleMassCreateDto req) {
        List<ScheduleDetailDto> result = scheduleService.massCreate(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.CREATED.value())
                        .status_message("스케줄 대량 생성 완료")
                        .build()
        );
    }

    @PostMapping("/mass-validate")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> massValidate(@Valid @RequestBody ScheduleMassCreateDto req) {
        Map<String, Object> result = scheduleService.massValidate(req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 대량 등록 사전검증 완료")
                        .build()
        );
    }

    @PatchMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> update(@PathVariable Long id, @Valid @RequestBody ScheduleUpdateDto req) {
        ScheduleDetailDto result = scheduleService.update(id, req);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 수정 완료")
                        .build()
        );
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 삭제 완료")
                        .build()
        );
    }

    @PostMapping("/delete-many")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> deleteMany(@RequestBody @NotEmpty List<@NotNull Long> scheduleIds) {
        scheduleService.deleteMany(scheduleIds);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result("ok")
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 일괄 삭제 완료")
                        .build()
        );
    }

    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> detail(@PathVariable Long id) {
        ScheduleDetailDto result = scheduleService.detail(id);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 상세 조회 완료")
                        .build()
        );
    }

    // 🔁 기간 기본값/검증은 서비스로 이관
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER')")
    public ResponseEntity<CommonSuccessDto> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<ScheduleListDto> result = scheduleService.listAllWithDefaults(from, to);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 목록(전사) 조회 완료")
                        .build()
        );
    }

    // 🔁 기간 기본값/검증은 서비스로 이관
    @GetMapping("/my-schedule")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> mySchedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<ScheduleListDto> result = scheduleService.listMineWithDefaults(from, to);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("내 스케줄 목록 조회 완료")
                        .build()
        );
    }

    // 기존 시그니처 유지
    @GetMapping(value = "/calendar", params = {"employeeId","yearMonth"})
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> calendar(
            @RequestParam @NotNull Long employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") @NotNull YearMonth yearMonth
    ) {
        List<ScheduleCalendarDto> result = scheduleService.calendar(employeeId, yearMonth.toString());
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 캘린더 조회 완료")
                        .build()
        );
    }

    // from/to 오버로드 → 검증을 서비스로 이관
    @GetMapping(value = "/calendar", params = {"from","to"})
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> calendarRangeFallback(
            @RequestParam(required = false) Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<ScheduleCalendarDto> result = scheduleService.calendarRangeFallback(employeeId, from, to);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 캘린더 범위 조회 완료(호환 경로)")
                        .build()
        );
    }

    // from/to 검증을 서비스로 이관
    @GetMapping("/calendar-range")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> calendarRange(
            @RequestParam(required = false) List<Long> employeeIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<ScheduleCalendarDto> result = scheduleService.calendarRangeValidated(employeeIds, from, to);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 캘린더 범위 조회 완료")
                        .build()
        );
    }
}
