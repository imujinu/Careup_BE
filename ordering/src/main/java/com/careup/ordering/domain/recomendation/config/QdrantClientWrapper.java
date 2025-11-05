package com.careup.ordering.domain.recomendation.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class QdrantClientWrapper {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public QdrantClientWrapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
    public List<Map<String, Object>> searchProductVector(float[] vector, int limit) {
        Map<String, Object> body = new HashMap<>();
        body.put("vector", toFloatList(vector));
        body.put("limit", limit);

        System.out.println("🔍 Qdrant 검색 요청 ===>");
        try {
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> response = webClient.post()
                .uri("/collections/products/points/search")
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println("❌ Qdrant 응답 에러:\n" + errorBody);
                                    return Mono.error(new RuntimeException(errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .block();

        // ✅ 결과 파싱
        if (response == null || response.get("result") == null) return Collections.emptyList();
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) response.get("result");

        System.out.println("✅ Qdrant 검색 결과 ===>");
        resultList.forEach(r -> System.out.println(r));

        return resultList;
    }

    private static List<Float> toFloatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float v : arr) list.add(v);
        return list;
    }
}