package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.ScheduleType;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleTypeDetailDto {
    private Long id;
    private String name;
    private ScheduleTypeCategory category;

    public static ScheduleTypeDetailDto fromEntity(ScheduleType e) {
        return ScheduleTypeDetailDto.builder()
                .id(e.getId())
                .name(e.getName())
                .category(e.getCategory())
                .build();
    }
}
