package com.careup.ordering.domain.recomendation.service;

import com.careup.ordering.domain.product.entity.*;
import com.careup.ordering.domain.product.repository.*;
import com.careup.ordering.domain.recomendation.config.QdrantClientWrapper;
import com.careup.ordering.domain.recomendation.config.VectorEncoder;
import com.careup.ordering.domain.recomendation.dto.ProductWithSimilarity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QdrantService {
    private final ProductRepository productRepository;
    private final QdrantClientWrapper qdrantClientWrapper;
    private final VectorEncoder vectorEncoder;
    private final AttributeValueRepository attributeValueRepository;
    
    /**
     * Qdrant 벡터 저장
     * 외부 서비스이므로 트랜잭션 없이 실행하여 DB 트랜잭션에 영향을 주지 않도록 함
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void saveProduct(Product product) {
        try {
            long price = (product.getMaxPrice() + product.getMinPrice()) / 2;
            Map<String, String> features = new HashMap<>();
            features.put("category", product.getCategory().getName());
            features.put("price", String.valueOf(price));

            // ProductAttributeValue가 null이거나 빈 리스트일 수 있음
            List<ProductAttributeValue> list = product.getProductAttributeValues();
            if (list != null && !list.isEmpty()) {
                for (ProductAttributeValue pa : list) {
                    try {
                        extracted(pa, features);
                    } catch (Exception e) {
                        log.warn("속성 값 추출 실패 - productId: {}, attributeValueId: {}, error: {}", 
                                product.getId(), 
                                pa != null && pa.getAttributeValue() != null ? pa.getAttributeValue().getId() : "unknown",
                                e.getMessage());
                        // 개별 속성 값 추출 실패해도 계속 진행
                    }
                }
            }

            float[] vector = vectorEncoder.encode(features, product);

            Map<String, Object> payload = new HashMap<>();
            payload.put("product_id", product.getId());
            payload.putAll(features);

            qdrantClientWrapper.upsertProductVector(product.getId(), vector, payload);
            log.info("Qdrant 벡터 저장 성공 - productId: {}", product.getId());
        } catch (Exception e) {
            log.error("Qdrant 벡터 저장 중 예외 발생 - productId: {}, error: {}", product.getId(), e.getMessage(), e);
            throw e; // 예외를 다시 던져서 호출자에서 처리하도록
        }
    }

    @Transactional(readOnly = true)
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
        if (pa == null || pa.getAttributeValue() == null) {
            log.warn("ProductAttributeValue 또는 AttributeValue가 null입니다.");
            return;
        }
        
        Long attributeValueId = pa.getAttributeValue().getId();
        AttributeValue av = attributeValueRepository.findById(attributeValueId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 상품 속성입니다. attributeValueId: " + attributeValueId));
        
        AttributeType at = av.getAttributeType();
        if (at != null && at.getName() != null) {
            features.put(at.getName(), av.getDisplayName() != null ? av.getDisplayName() : "");
        }
    }


}
