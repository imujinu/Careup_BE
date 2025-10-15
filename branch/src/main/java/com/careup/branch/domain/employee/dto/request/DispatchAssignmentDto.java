package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchAssignmentDto {
    @NotNull
    private Long branchId;
    @NotNull
    private LocalDate assignedFrom;
    @NotNull
    private LocalDate assignedTo;
    private String placementYn;
}
