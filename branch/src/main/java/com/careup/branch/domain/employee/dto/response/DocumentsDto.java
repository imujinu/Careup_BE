package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.DocumentType;
import com.careup.branch.domain.employee.entity.Documents;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentsDto {

    private Long id;
    private Long employeeId;                 // 엔터티 직접 노출 대신 ID만 노출
    private DocumentType documentType;
    private String title;
    private String documentUrl;      // 응답은 파일 자체가 아닌 URL 문자열 노출

    // Entity -> DTO
    public static DocumentsDto fromEntity(Documents documents) {
        return DocumentsDto.builder()
                .id(documents.getId())
                .employeeId(documents.getEmployee() != null ? documents.getEmployee().getId() : null)
                .documentType(documents.getDocumentType())
                .title(documents.getTitle())
                .documentUrl(documents.getDocumentUrl())
                .build();
    }
}
