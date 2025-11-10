package com.careup.branch.domain.employee.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentsListResDto {
    private List<DocumentsDto> data;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
    private boolean first;
    private boolean last;

    public static DocumentsListResDto fromPage(Page<DocumentsDto> page) {
        return DocumentsListResDto.builder()
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
