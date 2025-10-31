package com.careup.branch.common.api.controller;

import com.careup.branch.common.api.dto.response.HolidayDto;
import com.careup.branch.common.api.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CalendarController {

    private final HolidayService holidayService;

    @GetMapping("/calendar/holidays")
    public ResponseEntity<List<HolidayDto>> holidays(
            @RequestParam String from,
            @RequestParam String to
    ) {
        List<HolidayDto> list = holidayService.fetchKoreanHolidays(LocalDate.parse(from), LocalDate.parse(to));
        return ResponseEntity.ok(list);
    }
}
