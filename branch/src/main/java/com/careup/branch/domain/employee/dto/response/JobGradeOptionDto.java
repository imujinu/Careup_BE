package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.JobGrade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobGradeOptionDto {
    private Long id;
    private String name;

    public static JobGradeOptionDto fromEntity(JobGrade e) {
        return JobGradeOptionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .build();
    }
}
