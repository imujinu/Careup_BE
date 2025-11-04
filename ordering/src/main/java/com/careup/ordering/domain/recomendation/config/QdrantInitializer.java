package com.careup.ordering.domain.recomendation.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class QdrantInitializer {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        String url = "http://localhost:6333/collections/products";

        Map<String, Object> body = Map.of(
                "vectors", Map.of(
                        "size", 28,
                        "distance", "Cosine"
                )
        );

        try {
            restTemplate.put(url, body);
            log.info("✅ Qdrant collection 'products' created or already exists");
        } catch (Exception e) {
            log.error("❌ Qdrant collection init failed: {}", e.getMessage());
        }
    }
}
