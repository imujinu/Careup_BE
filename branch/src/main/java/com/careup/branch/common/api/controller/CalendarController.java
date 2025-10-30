package com.careup.branch.common.api.controller;

import java.time.LocalDate;
import java.util.List;

import com.careup.branch.common.api.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CalendarController {

    private final HolidayService holidayService;

    @GetMapping("/calendar/holidays")
    public ResponseEntity<List<String>> holidays(
            @RequestParam String from,
            @RequestParam String to
    ) {
        List<String> list = holidayService.fetchKoreanHolidays(LocalDate.parse(from), LocalDate.parse(to));
        return ResponseEntity.ok(list);
    }
}
