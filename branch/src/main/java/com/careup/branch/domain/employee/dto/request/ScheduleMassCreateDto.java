package com.careup.branch.domain.employee.dto.request;

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
    private List<ScheduleMassBlockDto> blocks;
    private List<ScheduleMassItemDto> items;
}
