package com.careup.branch.domain.employee.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeListDto {
    private int page;            // 0-based
    private int size;
    private int totalPages;
    private long totalElements;
    private List<EmployeeDetailDto> content;

    public static EmployeeListDto fromPage(Page<EmployeeDetailDto> p) {
        return EmployeeListDto.builder()
                .page(p.getNumber())
                .size(p.getSize())
                .totalPages(p.getTotalPages())
                .totalElements(p.getTotalElements())
                .content(p.getContent())
                .build();
    }
}
