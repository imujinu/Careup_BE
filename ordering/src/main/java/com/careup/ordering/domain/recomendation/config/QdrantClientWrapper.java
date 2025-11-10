package com.careup.ordering.domain.recomendation.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class QdrantClientWrapper {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
//    private final String apiKey;

    public QdrantClientWrapper(
            ObjectMapper objectMapper,
            @Value("${qdrant.host}") String host,
            @Value("${qdrant.port}") int port
//            @Value("${qdrant.api-key:}") String apiKey
    ) {
        this.objectMapper = objectMapper;
//        this.apiKey = apiKey;
        
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(String.format("http://%s:%d", host, port));
        
        // API 키가 있으면 기본 헤더에 추가 (api-key 또는 Authorization Bearer 형식 지원)
//        if (apiKey != null && !apiKey.trim().isEmpty()) {
//            builder.defaultHeader("api-key", apiKey);
//            // Authorization Bearer 형식도 지원 (Qdrant가 둘 다 지원)
////            builder.defaultHeader("Authorization", "Bearer " + apiKey);
//        }
        
        this.webClient = builder.build();
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