package com.careup.ordering.domain.recomendation.config;

import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductAttribute;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import com.careup.ordering.domain.product.repository.ProductAttributeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VectorEncoder {
    private final CategoryRepository categoryRepository;
    private final ProductAttributeRepository productAttributeRepository;

    public float[] encode(Map<String, String> features, Product product) {
        List<Float> vector = new ArrayList<>();

        List<String> categories = categoryRepository.findAll().stream().map(Category::getName).collect(Collectors.toList());
        List<ProductAttribute> productAttributes = productAttributeRepository.findAllByProductId(product.getId());

        // 예: category 원핫 인코딩
        if (product.getCategory() != null && product.getCategory().getName() != null) {
            vector.addAll(oneHot(product.getCategory().getName(), categories));
        } else {
            // 카테고리 없는 상품은 0으로 채워서 차원 유지
            vector.addAll(Collections.nCopies(categories.size(), 0f));
        }
        Map<String, List<String>> map = new HashMap<>();

        for (ProductAttribute pa : productAttributes) {
            map.computeIfAbsent(pa.getAttributeName(), k -> new ArrayList<>())
                    .add(pa.getAttributeValue());
        }
        for(String str : map.keySet()){
            vector.addAll(oneHot(str, map.get(str)));
        }
        // 가격 정규화
        if (features.containsKey("price")) {
            float price = Float.parseFloat(features.get("price"));
            vector.add(normalize(price, product.getMinPrice(), product.getMaxPrice()));
        }

        return toFloatArray(vector);
    }

    private List<Float> oneHot(String value, List<String> candidates) {
        List<Float> vec = new ArrayList<>(Collections.nCopies(candidates.size(), 0f));
        int idx = candidates.indexOf(value);
        if (idx >= 0) vec.set(idx, 1f);
        return vec;
    }

    private float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}
