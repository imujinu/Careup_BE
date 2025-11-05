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

        // [ 카테고리, 컬러, 분류, 계절, 성별 ] 순서

        String category = features.getOrDefault("category", "기타");
        vector.addAll(oneHot(category, List.of("상의", "하의", "아우터", "신발", "악세서리", "기타")));

        String color = features.getOrDefault("color", "기타");
        vector.addAll(oneHot(color, List.of("white", "black", "gray", "blue", "red", "green", "기타")));

        String useType = features.getOrDefault("useType", "일상");
        vector.addAll(oneHot(useType, List.of("러닝", "헬스", "축구", "요가", "등산", "일상")));

        String season = features.getOrDefault("season", "사계절");
        vector.addAll(oneHot(season, List.of("봄", "여름", "가을", "겨울", "사계절")));

        String gender = features.getOrDefault("gender", "공용");
        vector.addAll(oneHot(gender, List.of("남성", "여성", "공용")));

        // 6️⃣ Price normalization
        if (features.containsKey("price")) {
            float price = Float.parseFloat(features.get("price"));
            vector.add(normalize(price, 10000, 300000)); // 예: 1만~30만 원 사이
        } else {
            vector.add(0f);
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
