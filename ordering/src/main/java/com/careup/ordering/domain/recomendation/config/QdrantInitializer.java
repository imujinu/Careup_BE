package com.careup.ordering.domain.recomendation.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class QdrantInitializer {
    @Value("${qdrant.host}") String host;
    @Value("${qdrant.port}") int port;
    private final RestTemplate restTemplate = new RestTemplate();
//    @Value("${qdrant.api-key}") // 없을 수도 있으므로 기본값 빈 문자열
//    private String apiKey;

    @PostConstruct
    public void init() {
        String url = String.format("http://%s:%d/collections/products", host, port);

        Map<String, Object> body = Map.of(
                "vectors", Map.of(
                        "size", 28,
                        "distance", "Cosine"
                )
                        );
HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

//        System.out.println("api key ====" + apiKey);
//// ✅ Qdrant 인증 헤더 추가 (Cloud 환경 포함)
//        if (apiKey != null && !apiKey.isBlank()) {
//        headers.set("api-key", apiKey);
//        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

                try {
                    restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            log.info("✅ Qdrant collection 'products' created or already exists");
        } catch (Exception e) {
        log.error("❌ Qdrant collection init failed: {}", e.getMessage());
        }
        }
        }
