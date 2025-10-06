package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.JobGrade;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobGradeListDto {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JobGradeListDto fromEntity(JobGrade g) {
        return JobGradeListDto.builder()
                .id(g.getId())
                .name(g.getName())
                .createdAt(g.getCreatedAt())
                .updatedAt(g.getUpdatedAt())
                .build();
    }
}
