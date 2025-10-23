package com.careup.branch.domain.chat.repository;

import com.careup.branch.domain.chat.dto.DocumentSearchResultDto;
import com.careup.branch.domain.chat.service.DocumentProcessingService;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class InMemoryDocumentVectorStore {
    private final DocumentProcessingService documentProcessingService;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    public void addDocument(String id, String fileText, Map<String, Object> metadata) {
        log.info("문서 추가 시작 - ID: {}, 내용 길이: {}", id, fileText.length());
        log.info("fileText 내용: '{}'", fileText);


        try {
            Document document = new Document(fileText, Map.of("id", id));
            TokenTextSplitter textSplitter = TokenTextSplitter.builder()
                    .withChunkSize(512)
                    .withMinChunkSizeChars(350)
                    .withMinChunkLengthToEmbed(5)
                    .withMaxNumChunks(10000)
                    .withKeepSeparator(true)
                    .build();

            List<Document> chunks = textSplitter.split(document);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("문서 내용이 없거나 chunk 생성에 실패했습니다.");
            }

            vectorStore.add(chunks);


            log.info("문서 추가 완료 - ID: {}", id);
        } catch (Exception e) {
            log.error("문서 추가 실패 - ID: {}", id, e);
            throw new IllegalArgumentException("문서 임베딩 및 저장 실패: " + e.getMessage(), e);
        }
    }

    public void addDocumentFile(String id, File file, Map<String, Object> metadata) {
        log.debug("파일 문서 추가 시작 - ID: {}, 파일: {}", id, file.getName());


        try (InputStream inputStream = new FileInputStream(file)) {
            String fileText;

            // 파일 확장자 확인 후 PDF 여부 판단
            if (file.getName().toLowerCase().endsWith(".pdf")) {
                fileText = documentProcessingService.extractTextFromPdf(file);
            } else {
                // 일반 텍스트 파일일 경우
                fileText = new String(inputStream.readAllBytes());
            }

            log.debug("파일 텍스트 추출 완료 - 길이: {}", fileText.length());
            addDocument(id, fileText, metadata);

        } catch (Exception e) {
            log.error("파일 처리 실패 - ID: {}, 파일: {}", id, file.getName(), e);
            throw new IllegalArgumentException("파일 처리 실패: " + e.getMessage(), e);
        }
    }

    public List<DocumentSearchResultDto> similaritySearch(String query, int maxResults) {
        log.info("유사도 검색 시작 - 질의: '{}', 최대 결과: {}", query, maxResults);
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(maxResults)
                    .build();


            List<Document> results = vectorStore.similaritySearch(request);
            log.info("유사도 검색 완료 - 결과 수: {}", results.size());

            return results.stream().map(result -> new DocumentSearchResultDto(
                    result.getMetadata().getOrDefault("id", "unknown").toString(),
                    result.getText(),
                    result.getMetadata().entrySet().stream()
                            .filter(e -> !"id".equals(e.getKey()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                    result.getScore() != null ? result.getScore() : 0.0
            )).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("유사도 검색 실패 - 질의: '{}'", query, e);
            throw new IllegalArgumentException("유사도 검색 중 오류 발생: " + e.getMessage(), e);
        }
    }

}
