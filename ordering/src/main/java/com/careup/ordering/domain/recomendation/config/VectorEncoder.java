package com.careup.ordering.domain.recomendation.config;

import com.careup.ordering.domain.product.entity.AttributeValue;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductAttributeValue;
import com.careup.ordering.domain.product.repository.AttributeValueRepository;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import com.careup.ordering.domain.product.repository.ProductAttributeValueRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class VectorEncoder {
    private final CategoryRepository categoryRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final AttributeValueRepository attributeValueRepository;
    private static final Random RANDOM = new Random();
    public float[] encode(Map<String, String> features, Product product) {
        List<Float> vector = new ArrayList<>();

        // [ 카테고리, 컬러, 분류, 계절, 성별 ] 순서

        String category = product.getCategory().getName();
        vector.addAll(oneHot(category, List.of("상의", "하의", "아우터", "신발", "악세서리", "기타")));

        List<String> colorList = List.of("흰색", "회색", "검은색", "카키색", "베이직", "파란색", "기타");
        String color = features.getOrDefault("color", randomPick(colorList));
        vector.addAll(oneHot(color, colorList));

        List<String> sizeList = List.of("220", "230", "240", "250", "260", "270");
        String useType = features.getOrDefault("useType", randomPick(sizeList));
        vector.addAll(oneHot(useType, sizeList));

        List<String> seasonList = List.of("봄", "여름", "가을", "겨울", "사계절");
        String season = features.getOrDefault("season", randomPick(seasonList));
        vector.addAll(oneHot(season, seasonList));

        List<String> genderList = List.of("남성", "여성", "공용");
        String gender = features.getOrDefault("gender", randomPick(genderList));
        vector.addAll(oneHot(gender, genderList));

        // 6️⃣ Price normalization
        if (features.containsKey("price")) {
            float price = Float.parseFloat(features.get("price"));
            vector.add(normalize(price, 10000, 300000)); // 예: 1만~30만 원 사이
        } else {
            vector.add(0f);
        }

        return toFloatArray(vector);
    }
    private static <T> T randomPick(List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
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
