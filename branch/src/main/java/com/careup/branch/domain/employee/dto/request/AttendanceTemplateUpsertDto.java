package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceTemplateUpsertDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    // 시간들은 선택 입력 (null 허용)
    private LocalTime defaultClockIn;
    private LocalTime defaultBreakStart;
    private LocalTime defaultBreakEnd;
    private LocalTime defaultClockOut;
}
