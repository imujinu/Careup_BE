package com.careup.branch.domain.employee.dto.response;

import com.careup.branch.domain.employee.entity.DocumentStatus;
import com.careup.branch.domain.employee.entity.DocumentType;
import com.careup.branch.domain.employee.entity.Documents;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentsDto {

    private Long id;
    private Long employeeId;                 // 엔터티 직접 노출 대신 ID만 노출
    private DocumentType documentType;
    private String title;
    private String documentUrl;              // 응답은 파일 자체가 아닌 URL 문자열 노출
    private Long fileSize;                   // 파일 크기 (바이트)
    private LocalDateTime uploadedAt;        // 업로드일 (createdAt)
    private LocalDate expiryDate;            // 만료일 (선택)
    private String description;              // 설명 (선택)
    private DocumentStatus documentStatus;   // 서류 상태

    // Entity -> DTO
    public static DocumentsDto fromEntity(Documents documents) {
        return DocumentsDto.builder()
                .id(documents.getId())
                .employeeId(documents.getEmployee() != null ? documents.getEmployee().getId() : null)
                .documentType(documents.getDocumentType())
                .title(documents.getTitle())
                .documentUrl(documents.getDocumentUrl())
                .fileSize(documents.getFileSize())
                .uploadedAt(documents.getCreatedAt())
                .expiryDate(documents.getExpiryDate())
                .description(documents.getDescription())
                .documentStatus(documents.getDocumentStatus())
                .build();
    }
}
