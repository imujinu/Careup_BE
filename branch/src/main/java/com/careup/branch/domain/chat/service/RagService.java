package com.careup.branch.domain.chat.service;

import com.careup.branch.domain.chat.config.VectorStoreConfig;
import com.careup.branch.domain.chat.dto.DocumentSearchResultDto;
import com.careup.branch.domain.chat.repository.InMemoryDocumentVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RagService {


    private QdrantVectorStore vectorStore;
    private final InMemoryDocumentVectorStore inMemoryDocumentVectorStore;

    public void uploadPdfFile(String documentId, File file, String originalFilename) {
        if (file == null || file.length() == 0) {
            throw new IllegalArgumentException("업로드된 파일이 비어있습니다.");
        }
        log.info("PDF 문서 업로드 시작. 파일: {}, ID: {}", originalFilename, documentId);

        Map<String, Object> docMetadata = new HashMap<>();
        docMetadata.put("originalFilename", originalFilename != null ? originalFilename : "");
        docMetadata.put("uploadTime", System.currentTimeMillis());

        try {
            inMemoryDocumentVectorStore.addDocumentFile(documentId, file, docMetadata);
            log.info("PDF 문서 업로드 완료. ID: {}", documentId);
        } catch (Exception e) {
            log.error("문서 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new IllegalArgumentException("문서 처리 중 오류: " + e.getMessage(), e);
        }
    }
    public String retrieve(Long documentId, String question, int maxResult){

        log.info("검색 시작 : {}, 최대 결과 수 : {} ", question, maxResult);
        String result =  inMemoryDocumentVectorStore.similaritySearch(documentId, question, maxResult);
        return result;
    }



    private void validatePdfFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // PDF만 허용
        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("PDF 파일만 업로드 가능합니다.");
        }
    }



}
