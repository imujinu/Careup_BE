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
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
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

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> list(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<ScheduleListDto> result = scheduleService.list(employeeId, from, to);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 목록 조회 완료")
                        .build()
        );
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasAnyRole('HQ_ADMIN','BRANCH_ADMIN','FRANCHISE_OWNER','STAFF')")
    public ResponseEntity<CommonSuccessDto> calendar(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") String yearMonth
    ) {
        List<ScheduleCalendarDto> result = scheduleService.calendar(employeeId, yearMonth);
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("스케줄 캘린더 조회 완료")
                        .build()
        );
    }
}
