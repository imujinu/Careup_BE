package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTypeUpsertDto {
    @NotBlank
    @Size(max = 20)
    private String name;
    private Boolean geofenceRequired;
    private Integer geofenceRadiusMeters;
}
