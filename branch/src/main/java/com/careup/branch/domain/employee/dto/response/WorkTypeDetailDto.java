package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.WorkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTypeDetailDto {
    private Long id;
    private String name;
    private Boolean geofenceRequired;

    public static WorkTypeDetailDto fromEntity(WorkType e) {
        return WorkTypeDetailDto.builder()
                .id(e.getId())
                .name(e.getName())
                .geofenceRequired(e.getGeofenceRequired())
                .build();
    }
}
