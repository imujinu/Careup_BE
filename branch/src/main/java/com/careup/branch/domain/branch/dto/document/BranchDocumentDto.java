package com.careup.branch.domain.branch.dto.document;

import com.careup.branch.domain.branch.entity.BranchDocumentType;
import com.careup.branch.domain.branch.entity.BranchDocuments;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDocumentDto {

    private Long id;
    private Long branchId;                 // 엔터티 직접 노출 대신 ID만 노출
    private BranchDocumentType branchDocumentType;
    private String title;
    private String branchDocumentUrl;      // 응답은 파일 자체가 아닌 URL 문자열 노출

    // Entity -> DTO
    public static BranchDocumentDto fromEntity(BranchDocuments branchDocuments) {
        return BranchDocumentDto.builder()
                .id(branchDocuments.getId())
                .branchId(branchDocuments.getBranch() != null ? branchDocuments.getBranch().getId() : null)
                .branchDocumentType(branchDocuments.getBranchDocumentType())
                .title(branchDocuments.getTitle())
                .branchDocumentUrl(branchDocuments.getBranchDocumentUrl())
                .build();
    }
}
