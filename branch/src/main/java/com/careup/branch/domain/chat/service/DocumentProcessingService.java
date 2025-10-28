package com.careup.branch.domain.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class DocumentProcessingService {

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
