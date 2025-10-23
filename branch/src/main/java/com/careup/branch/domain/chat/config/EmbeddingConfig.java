package com.careup.branch.domain.chat.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;

import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class EmbeddingConfig {

    private final OpenAiApi openAiApi;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String embeddingModel;

    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.grpc-port:6334}")
    private int qdrantGrpcPort;

    @Bean
    public QdrantClient qdrantClient() {
        // ⚠️ 직접 생성이 아닌 정적 메서드 사용
        QdrantGrpcClient grpcClient = QdrantGrpcClient.(qdrantHost, qdrantGrpcPort);
        return new QdrantClient(grpcClient);
    }
    @Bean
    public VectorStore defaultVectorStore() {
        return QdrantVectorStore.builder()
                .host(qdrantHost)
                .port(qdrantPort)
                .collectionName("default_store")
                .embeddingModel(embeddingModel)
                .build();
    }

    /**
     * ✅ 사용자별 컬렉션을 동적으로 생성
     */
    public VectorStore createUserVectorStore(Long userId) {
        return QdrantVectorStore.builder()
                .host(qdrantHost)
                .port(qdrantPort)
                .collectionName("user_" + userId)
                .embeddingModel(embeddingModel)
                .build();
    }
}