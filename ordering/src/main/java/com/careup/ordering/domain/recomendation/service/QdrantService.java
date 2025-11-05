package com.careup.ordering.domain.recomendation.service;

import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductAttribute;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import com.careup.ordering.domain.product.repository.ProductAttributeRepository;
import com.careup.ordering.domain.product.repository.ProductRepository;
import com.careup.ordering.domain.recomendation.config.QdrantClientWrapper;
import com.careup.ordering.domain.recomendation.config.VectorEncoder;
import com.careup.ordering.domain.recomendation.dto.ProductWithSimilarity;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
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
    private final ProductAttributeRepository productAttributeRepository;
    private final QdrantClientWrapper qdrantClientWrapper;
    private final VectorEncoder vectorEncoder;
    private final ProductRepository productRepository;

    @Transactional
    public void saveProduct(Product product) {
        long price = (product.getMaxPrice() + product.getMinPrice()) / 2;
        Map<String, String> features = new HashMap<>();
        features.put("category", product.getCategory().getName());
        features.put("price", String.valueOf(price));

        List<ProductAttribute> list = productAttributeRepository.findAllByProductId(product.getId());
        for (ProductAttribute pa : list) {
            features.put(pa.getAttributeName(), pa.getAttributeValue());
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

        productAttributeRepository.findAllByProductId(product.getId())
                .forEach(pa -> features.put(pa.getAttributeName(), pa.getAttributeValue()));

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




}
