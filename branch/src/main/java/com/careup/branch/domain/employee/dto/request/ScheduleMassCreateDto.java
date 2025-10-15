package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleMassCreateDto {

    @Valid
    private List<ScheduleMassBlockDto> blocks;

    @Valid
    private List<ScheduleMassItemDto> items;
}
