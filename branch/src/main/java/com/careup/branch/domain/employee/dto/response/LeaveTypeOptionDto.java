// src/main/java/com/careup/branch/domain/employee/dto/response/LeaveTypeOptionDto.java
package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.LeaveType;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeOptionDto {
    private Long id;
    private String name;
    private Boolean paid;

    public static LeaveTypeOptionDto fromEntity(LeaveType e) {
        return LeaveTypeOptionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .paid(e.getPaid())
                .build();
    }
}
