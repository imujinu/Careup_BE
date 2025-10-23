package com.careup.branch.domain.chat.config;

import io.micrometer.observation.ObservationRegistry;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {

    private final OpenAiApi openAiApi;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String embeddingModelName;

    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.port}")
    private int qdrantPort;

    private final OpenAiEmbeddingModel embeddingModel;

    @Bean
    public QdrantClient qdrantClient() {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder("localhost", 6334, false).build()
        );
    }
}
