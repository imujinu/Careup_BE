// src/main/java/com/careup/branch/domain/employee/dto/response/WorkTypeOptionDto.java
package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.WorkType;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTypeOptionDto {
    private Long id;
    private String name;
    private Boolean geofenceRequired;

    public static WorkTypeOptionDto fromEntity(WorkType e) {
        return WorkTypeOptionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .geofenceRequired(e.getGeofenceRequired())
                .build();
    }
}
