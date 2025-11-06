package com.careup.ordering.domain.recomendation.service;

import com.careup.ordering.domain.product.entity.*;
import com.careup.ordering.domain.product.repository.*;
import com.careup.ordering.domain.recomendation.config.QdrantClientWrapper;
import com.careup.ordering.domain.recomendation.config.VectorEncoder;
import com.careup.ordering.domain.recomendation.dto.ProductWithSimilarity;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QdrantService {
    private final ProductRepository productRepository;
    private final QdrantClientWrapper qdrantClientWrapper;
    private final VectorEncoder vectorEncoder;
    private final AttributeValueRepository attributeValueRepository;
    @Transactional
    public void saveProduct(Product product) {
        long price = (product.getMaxPrice() + product.getMinPrice()) / 2;
        Map<String, String> features = new HashMap<>();
        features.put("category", product.getCategory().getName());
        features.put("price", String.valueOf(price));

        List<ProductAttributeValue> list = product.getProductAttributeValues();
        for (ProductAttributeValue pa : list) {
            extracted(pa, features);
        }

        float[] vector = vectorEncoder.encode(features, product);

        Map<String, Object> payload = new HashMap<>();
        payload.put("product_id", product.getId());
        payload.putAll(features);

        qdrantClientWrapper.upsertProductVector(product.getId(), vector, payload);
    }

    public List<ProductWithSimilarity> searchSimilarProducts(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 1️⃣ feature → vector 인코딩
        Map<String, String> features = new HashMap<>();
        features.put("category", product.getCategory().getName());
        long price = (product.getMaxPrice() + product.getMinPrice()) / 2;
        features.put("price", String.valueOf(price));

        product.getProductAttributeValues()
                .forEach(pa -> {
                    extracted(pa, features);
                });

        float[] queryVector = vectorEncoder.encode(features, product);

        // 2️⃣ Qdrant에서 유사 상품 검색
        List<Map<String, Object>> results = qdrantClientWrapper.searchProductVector(queryVector, limit);

        // 3️⃣ payload에서 product_id, score 추출
        List<ProductWithSimilarity> list = results.stream()
                .map(result -> {
                    Object idObj = result.get("id");
                    if (idObj == null) return null;

                    Long id = ((Number) idObj).longValue();
                    Double score = (Double) result.get("score");

                    return productRepository.findById(id)
                            .map(p -> new ProductWithSimilarity(p, score))
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return list;
    }

    private void extracted(ProductAttributeValue pa, Map<String, String> features) {
        AttributeValue av = attributeValueRepository.findById(pa.getAttributeValue().getId()).orElseThrow(()-> new EntityNotFoundException("존재 하지 않는 상품 속성입니다."));
        AttributeType at = av.getAttributeType();
        features.put(at.getName(), av.getValue());
    }


}
