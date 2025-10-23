package com.careup.branch.domain.chat.service;

import com.careup.branch.domain.chat.dto.DocumentSearchResultDto;
import com.careup.branch.domain.chat.repository.InMemoryDocumentVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;

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

    private final InMemoryDocumentVectorStore vectorStore;
    private final ChatService chatService;

    public String uploadPdfFile(File file, String originalFilename) {
        if (file == null || file.length() == 0) {
            throw new IllegalArgumentException("업로드된 파일이 비어있습니다.");
        }

        String documentId = UUID.randomUUID().toString();
        log.info("PDF 문서 업로드 시작. 파일: {}, ID: {}", originalFilename, documentId);

        Map<String, Object> docMetadata = new HashMap<>();
        docMetadata.put("originalFilename", originalFilename != null ? originalFilename : "");
        docMetadata.put("uploadTime", System.currentTimeMillis());

        try {
            vectorStore.addDocumentFile(documentId, file, docMetadata);
            log.info("PDF 문서 업로드 완료. ID: {}", documentId);
            return documentId;
        } catch (Exception e) {
            log.error("문서 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new IllegalArgumentException("문서 처리 중 오류: " + e.getMessage(), e);
        }
    }
    public List<DocumentSearchResultDto> retrieve(String question, int maxResult){

        log.info("검색 시작 : {}, 최대 결과 수 : {} ", question, maxResult);
        return vectorStore.similaritySearch(question, maxResult);
    }


    public String generateAnswerWithContexts(String question, List<DocumentSearchResultDto> relevantDocs) {


        log.info("RAG 응답 생성 시작: '{}'", question);

        if (relevantDocs.isEmpty()) {
            log.info("관련 정보를 찾을 수 없음: '{}'", question);
            return "관련 정보를 찾을 수 없습니다. 다른 질문을 시도하거나 관련 문서를 업로드해 주세요.";
        }

        // 문서 번호 부여
        List<String> numberedDocs = relevantDocs.stream()
                .map(doc -> "[" + (relevantDocs.indexOf(doc) + 1) + "] " + doc.getContent())
                .collect(Collectors.toList());

        // 컨텍스트 결합
        String context = String.join("\n\n", numberedDocs);
        log.info("컨텍스트 크기: {} 문자", context.length());

        // 시스템 프롬프트 생성
        String systemPromptText = String.format(
                "당신은 지식 기반 Q&A 시스템입니다.\n" +
                        "사용자의 질문에 대한 답변을 다음 정보를 바탕으로 생성해주세요.\n" +
                        "주어진 정보에 답이 없다면 모른다고 솔직히 말해주세요.\n" +
                        "답변 마지막에 사용한 정보의 출처 번호 [1], [2] 등을 반드시 포함해주세요.\n\n" +
                        "정보:\n%s", context
        );

        try {

            long step1Start = System.currentTimeMillis();
            String response = chatService.getResponseAsync(question, systemPromptText);
            log.info("챗봇 응답 완료 - 걸린 시간: {} ms", System.currentTimeMillis() - step1Start);
            log.info("AI 응답 생성: {}", response);

            String aiAnswer = response != null ? response : "응답을 생성할 수 없습니다.";

            // 참고 문서 정보 추가
            StringBuilder sourceInfo = new StringBuilder("\n\n참고 문서:\n");
            for (int i = 0; i < relevantDocs.size(); i++) {
                DocumentSearchResultDto doc = relevantDocs.get(i);
                String originalFilename = doc.getMetadata().getOrDefault("originalFilename", "Unknown file").toString();
                sourceInfo.append("[").append(i + 1).append("] ").append(originalFilename).append("\n");
            }

            return aiAnswer + sourceInfo.toString();

        } catch (Exception e) {
            log.info("AI 모델 호출 중 오류 발생: {}", e.getMessage(), e);
            String searchResults = relevantDocs.stream()
                    .map(DocumentSearchResultDto::getContent)
                    .collect(Collectors.joining("\n\n"));
            return "AI 모델 호출 중 오류가 발생했습니다. 검색 결과만 제공합니다:\n\n" + searchResults;
        }
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
