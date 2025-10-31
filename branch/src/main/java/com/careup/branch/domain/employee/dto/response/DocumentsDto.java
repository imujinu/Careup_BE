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
    private String branchName;               // 지점 이름
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
        String branchName = null;

        // 1. 먼저 Documents에 직접 연결된 Branch에서 가져오기
        if (documents.getBranch() != null) {
            branchName = documents.getBranch().getName();
        }
        // 2. Branch가 없으면 Employee의 DispatchStatus에서 가져오기
        else if (documents.getEmployee() != null) {
            // 현재 활성화된 지점 정보 추출 (placementYn = "N"인 DispatchStatus)
            branchName = documents.getEmployee().getDispatchStatuses().stream()
                    .filter(ds -> "N".equals(ds.getPlacementYn()))
                    .filter(ds -> ds.getBranch() != null)
                    .map(ds -> ds.getBranch().getName())
                    .findFirst()
                    .orElse(null);
        }

        return DocumentsDto.builder()
                .id(documents.getId())
                .branchName(branchName)
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
