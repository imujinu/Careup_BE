package com.careup.branch.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadFileDto {
    private File file;
    private String documentId;
    private String fileName;

    public UploadFileDto makeDto(File file, String documentId, String fileName){
        return UploadFileDto.builder()
                .file(file)
                .documentId(documentId)
                .fileName(fileName)
                .build();
    }
}
