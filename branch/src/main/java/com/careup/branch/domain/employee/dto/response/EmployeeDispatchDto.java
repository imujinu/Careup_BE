package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDispatchDto {
    private Long id;
    private Long branchId;
    private String branchName;
    private LocalDate assignedFrom;
    private LocalDate assignedTo;
    private String placementYn;

    public static EmployeeDispatchDto fromEntity(DispatchStatus d) {
        Branch b = d.getBranch();
        return EmployeeDispatchDto.builder()
                .id(d.getId())
                .branchId(b != null ? b.getId() : null)
                .branchName(b != null ? b.getName() : null)
                .assignedFrom(d.getAssignedFrom())
                .assignedTo(d.getAssignedTo())
                .placementYn(d.getPlacementYn())
                .build();
    }
}
