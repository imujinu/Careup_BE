package com.careup.branch.domain.chat.service;

import com.careup.branch.domain.chat.config.VectorStoreConfig;
import com.careup.branch.domain.chat.dto.DocumentChunk;
import com.careup.branch.domain.chat.dto.DocumentSearchResultDto;
import com.careup.branch.domain.chat.dto.SearchRequest;
import com.careup.branch.domain.chat.entity.ChatMessage;
import com.careup.branch.domain.chat.repository.InMemoryDocumentVectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RagService {


    private QdrantVectorStore vectorStore;
    private final InMemoryDocumentVectorStore inMemoryDocumentVectorStore;
    private final QdrantService qdrantService;
    private final OpenAiChatModel chatModel;
    private final OpenAiEmbeddingModel embeddingModel;

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



    public String askQuestion(SearchRequest request) {
        float[] embedArray = embeddingModel.embed(request.getQuery());

        List<Float> denseVector = new ArrayList<>();
        for (float v : embedArray) {
            denseVector.add(v);
        }

        Map<Long, Float> sparseVector = Collections.emptyMap();

        List<DocumentChunk> childChunks = qdrantService.searchHybrid(request, denseVector, sparseVector);

        String fullContext = childChunks.stream()
                .map(child -> qdrantService.getParentContent(child.getParentId()))
                .distinct()
                .collect(Collectors.joining("\n\n"));

        String promptText = String.format(
                "아래 제공된 [매뉴얼 문맥]을 바탕으로 사용자의 질문에 답변하세요.\n" +
                        "문맥에 없는 내용은 '모른다'고 답변하여 할루시네이션을 방지하세요.\n\n" +
                        "[매뉴얼 문맥]\n%s\n\n[사용자 질문]\n%s",
                fullContext, request.getQuery());

        return chatModel.call(promptText);
    }

}
