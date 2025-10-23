package com.careup.branch.domain.chat.controller;

import com.careup.branch.common.config.SecurityConfig;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.chat.config.VectorStoreConfig;
import com.careup.branch.domain.chat.service.RagService;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import io.micrometer.observation.ObservationRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
@Tag(name = "RAG API", description = "Retrieval-Augmented Generation 기능을 위한 API")
public class RagController {
    private final RagService ragService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @Parameter(description = "업로드할 PDF 파일", required = true)
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("파일이 비어있습니다.");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            return ResponseEntity.badRequest().body("PDF 파일만 업로드 가능합니다.");
        }

        log.info("문서 업로드 요청 받음: {}", file.getOriginalFilename());

        // 임시 파일 생성
        try {
            java.io.File tempFile = java.io.File.createTempFile("upload_", ".pdf");
            file.transferTo(tempFile);
            log.debug("임시 파일 생성됨: {}", tempFile.getAbsolutePath());

            // 서비스 호출
            String documentId = ragService.uploadPdfFile(tempFile, file.getOriginalFilename());

            log.info("문서 업로드 성공: {}", documentId);
            return new ResponseEntity<>(
                    new CommonSuccessDto(documentId, HttpStatus.ACCEPTED.value(), "문서 업로드 완료"),
                    HttpStatus.ACCEPTED
            );

        } catch (IOException e) {
            log.error("임시 파일 생성 또는 업로드 처리 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }



}
