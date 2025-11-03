package com.careup.ordering.domain.recomendation.service;

import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductAttribute;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import com.careup.ordering.domain.product.repository.ProductAttributeRepository;
import com.careup.ordering.domain.recomendation.config.QdrantClientWrapper;
import com.careup.ordering.domain.recomendation.config.VectorEncoder;
import io.qdrant.client.QdrantClient;
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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class QdrantService {
    private final ProductAttributeRepository productAttributeRepository;
    private final QdrantClientWrapper qdrantClientWrapper;
    private final VectorEncoder vectorEncoder;

    @Transactional
    public void saveProduct(Product product) {
        Long price = product.getMaxPrice()+product.getMinPrice()/2;
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

}
