package com.careup.branch.domain.chat.service;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.chat.dto.UploadFileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentProcessingService {
    public UploadFileDto uploadPdfFile(Long id, MultipartFile file){

    // 임시 파일 생성
        try {
        java.io.File tempFile = java.io.File.createTempFile("upload_", ".pdf");
        file.transferTo(tempFile);
        log.debug("임시 파일 생성됨: {}", tempFile.getAbsolutePath());
        String documentId = String.valueOf(id);

        log.info("문서 업로드 성공: {}", documentId);
        return new UploadFileDto().makeDto(tempFile, documentId, file.getOriginalFilename());
    } catch (IOException e) {
        log.error("임시 파일 생성 또는 업로드 처리 실패", e);
        throw  new IllegalArgumentException("문서 처리 실패");
    }
    }

    public String extractTextFromPdf(File file) {
        try (PDDocument document = PDDocument.load(file)) {
            log.debug("PDF 문서 로드 성공: {} 페이지", document.getNumberOfPages());

            PositionAwarePDFTextStripper stripper = new PositionAwarePDFTextStripper();
            stripper.getText(document); // writeString 호출
            String text = stripper.getSortedText();

            log.info("PDF 텍스트 추출 완료: {} 문자", text);
            return text;
        } catch (IOException e) {
            log.error("PDF 텍스트 추출 실패", e);
            throw new RuntimeException("PDF에서 텍스트 추출 실패: " + e.getMessage(), e);
        }
    }
}
