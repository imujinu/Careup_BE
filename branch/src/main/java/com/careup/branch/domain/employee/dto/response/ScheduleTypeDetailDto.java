package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.ScheduleType;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleTypeDetailDto {
    private Long id;
    private String name;

    public static ScheduleTypeDetailDto fromEntity(ScheduleType e) {
        return ScheduleTypeDetailDto.builder()
                .id(e.getId())
                .name(e.getName())
                .build();
    }
}
