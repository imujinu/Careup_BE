package com.careup.ordering.domain.recomendation.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class QdrantClientWrapper {

    private final WebClient webClient;

    public QdrantClientWrapper() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:6333") // Qdrant 서버 주소
                .build();
    }

    public void upsertProductVector(Long productId, float[] vector, Map<String, Object> payload) {
        List<Float> vectorList = new ArrayList<>();
        for (float v : vector) vectorList.add(v);

        Map<String, Object> point = new HashMap<>();
        point.put("id", productId);
        point.put("vector", vectorList);
        point.put("payload", payload);

        Map<String, Object> body = new HashMap<>();
        body.put("points", List.of(point));

        System.out.println("🚀 Qdrant Request Body ===>");
        try {
            System.out.println(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        webClient.put()
                .uri("/collections/products/points?wait=true")
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("❌ Qdrant 응답 에러 바디:\n" + errorBody);
                                    return Mono.error(new RuntimeException(errorBody));
                                })
                )
                .bodyToMono(String.class)
                .doOnNext(resp -> System.out.println("✅ Qdrant 응답: " + resp))
                .doOnError(err -> System.err.println("❌ Qdrant 에러: " + err.getMessage()))
                .block();
    }
}