package com.careup.branch.domain.branch.dto.document;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchDocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDocumentCreateReqDto {
//
//    // 하위 호환: branch.id 또는 단일 필드 branchId 둘 다 허용
//    private Branch branch;          // form-data 키: branch.id 로 바인딩 가능
//    private Long branchId;          // form-data 키: branchId 로 바인딩 가능 (권장)

    private BranchDocumentType branchDocumentType;
    private String title;
    private MultipartFile branchDocumentUrl; // 업로드 파일로 변경


}
