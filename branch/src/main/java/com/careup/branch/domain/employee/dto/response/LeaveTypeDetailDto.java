package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeDetailDto {
    private Long id;
    private String name;
    private Boolean paid;

    public static LeaveTypeDetailDto fromEntity(LeaveType e) {
        return LeaveTypeDetailDto.builder()
                .id(e.getId())
                .name(e.getName())
                .paid(e.getPaid())
                .build();
    }
}
