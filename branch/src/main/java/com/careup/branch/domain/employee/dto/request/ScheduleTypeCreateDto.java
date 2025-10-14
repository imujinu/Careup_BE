package com.careup.branch.domain.employee.dto.request;

import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleTypeCreateDto {
    @NotBlank
    @Size(max = 20)
    private String name;
    @NotNull
    private ScheduleTypeCategory category;
}
