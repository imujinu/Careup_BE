package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchAssignmentDto {
    @NotNull
    private Long branchId;
    @NotNull
    private LocalDate assignedFrom;
    @NotNull
    private LocalDate assignedTo;
    private String placementYn;
}
