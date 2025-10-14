package com.careup.branch.domain.employee.dto.request;

import com.careup.branch.domain.employee.entity.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentsCreateReqDto {

    private DocumentType documentType;
    private String title;
    private MultipartFile documentUrl; // 업로드 파일로 변경

}
