package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceTemplateDetailDto {
    private Long id;
    private String name;
    private LocalTime defaultClockIn;
    private LocalTime defaultBreakStart;
    private LocalTime defaultBreakEnd;
    private LocalTime defaultClockOut;

    public static AttendanceTemplateDetailDto fromEntity(AttendanceTemplate e) {
        return AttendanceTemplateDetailDto.builder()
                .id(e.getId())
                .name(e.getName())
                .defaultClockIn(e.getDefaultClockIn())
                .defaultBreakStart(e.getDefaultBreakStart())
                .defaultBreakEnd(e.getDefaultBreakEnd())
                .defaultClockOut(e.getDefaultClockOut())
                .build();
    }
}
