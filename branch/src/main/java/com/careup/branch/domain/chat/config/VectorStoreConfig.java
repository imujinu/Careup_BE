package com.careup.branch.domain.chat.config;

import io.micrometer.observation.ObservationRegistry;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {



    private final OpenAiEmbeddingModel embeddingModel;

    @Bean
    public VectorStore vectorStore() {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
