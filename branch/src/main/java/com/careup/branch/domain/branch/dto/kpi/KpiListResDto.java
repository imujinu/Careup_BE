package com.careup.branch.domain.branch.dto.kpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiListResDto {

    private List<KpiDto> data;
    private int currentPage; // 현재 페이지 (0부터 시작)
    private int totalPages; // 전체 페이지 수
    private long totalElements; // 전체 요소 수
    private int size; // 페이지당 요소 수
    private boolean first; // 첫 번째 페이지 여부
    private boolean last; // 마지막 페이지 여부

    public static KpiListResDto fromPage(Page<KpiDto> page) {
        return KpiListResDto.builder()
                .data(page.getContent())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
