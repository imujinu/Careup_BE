package com.careup.ordering.domain.recomendation.service;

import com.careup.ordering.domain.member.service.MemberQueryService;
import com.careup.ordering.domain.order.dto.response.ProductViewCountResDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.repository.ProductRepository;
import com.careup.ordering.domain.recomendation.dto.*;
import com.careup.ordering.domain.recomendation.entity.ProductCoPurchase;
import com.careup.ordering.domain.recomendation.repository.ProductCoPurchaseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CoPurchaseService {

    private final ProductCoPurchaseRepository coPurchaseRepository;
    private final MemberQueryService memberQueryService;
    @Qualifier("coPurchaseRedisTemplate")
    private final RedisTemplate<String, Long> redisTemplate;
    @Autowired
    private final StreamsBuilderFactoryBean factoryBean;
    private final QdrantService qdrantService;
    private final ProductRepository productRepository;
    public void updateCoPurchaseByOrder(List<Long> productIds) {
        if (productIds == null || productIds.size() < 2) return;

        for (int i = 0; i < productIds.size(); i++) {
            for (int j = i + 1; j < productIds.size(); j++) {
                Long a = Math.min(productIds.get(i), productIds.get(j));
                Long b = Math.max(productIds.get(i), productIds.get(j));

                // 함께 구매된 쌍 카운팅
                ProductCoPurchase record = coPurchaseRepository
                        .findByProductAIdAndProductBId(a, b)
                        .orElseGet(() -> ProductCoPurchase.builder()
                                .productAId(a)
                                .productBId(b)
                                .coPurchaseCount(0L)
                                .build());

                record.increaseCount();
                coPurchaseRepository.save(record);
            }
        }
    }




    public Object getCoPurchaseList(Long memberId, Pageable pageable) {

        ProductViewResultDto dto = memberQueryService.getProductId(memberId);

        if(!dto.isExist()){
            return getProducts(pageable);

        }

        Long productId = dto.getProductId();
        Product lastViewProduct = productRepository.findById(productId).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 상품입니다."));
        List<ProductWithSimilarity> similarProducts = qdrantService.searchSimilarProducts(productId, 30);
        List<Map.Entry<String, Long>> purchasedProducts = getCoPurchasedProducts(productId);
        Map<Long, Long> coPurchaseMap = purchasedProducts.stream()
                .collect(Collectors.toMap(
                        e -> Long.valueOf(e.getKey()),
                        Map.Entry::getValue
                ));

        long maxCount = coPurchaseMap.values().stream().max(Long::compare).orElse(1L);
        Map<Long, Double> normalized = coPurchaseMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() / (double) maxCount
                ));

        List<ProductWithScore> scored = similarProducts.stream()
                .map(pws -> {
                    double similarity = pws.getSimilarityScore();
                    double coPurchase = normalized.getOrDefault(pws.getProduct().getId(), 0.0);
                    double finalScore = (similarity * 0.6) + (coPurchase * 0.4);
                    return new ProductWithScore(pws.getProduct(), finalScore);
                })
                .sorted(Comparator.comparingDouble(ProductWithScore::getFinalScore).reversed())
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), scored.size());
        List<ProductCoPurchaseResDto.CoPurchaseDto> pageContent = scored.subList(start, end).stream()
                .map(pws -> {
                    Product product = pws.getProduct();
                    Long coPurchaseCount = coPurchaseMap.getOrDefault(product.getId(), 0L);
                    return ProductCoPurchaseResDto.CoPurchaseDto.makeDto(product, coPurchaseCount);
                })
                .collect(Collectors.toList());

        Page<ProductCoPurchaseResDto.CoPurchaseDto> page = new PageImpl<>(pageContent, pageable, scored.size());

        return ProductCoPurchaseResDto.builder()
                .products(page)
                .hasRecentView(true)
                .lastViewProductName(lastViewProduct.getName())
                .build();
    }



    public List<Map.Entry<String, Long>> getCoPurchasedProducts(Long productId) {
        String pattern = "co_purchase_count:*";
        Map<String, Long> coPurchaseMap = new HashMap<>();

        // Redis에서 모든 co_purchase_count key 스캔
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
        try (Cursor<byte[]> cursor = (Cursor<byte[]>) redisTemplate.getConnectionFactory()
                .getConnection()
                .keyCommands()
                .scan(options)) {

            while (cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8); // co_purchase_count:123-456
                String pair = key.replace("co_purchase_count:", "");
                String[] parts = pair.split("-");
                if (parts.length != 2) continue;

                String left = parts[0];
                String right = parts[1];

                // 특정 상품이 포함된 쌍만 필터링
                if (left.equals(productId.toString()) || right.equals(productId.toString())) {
                    if (left.equals(right)) continue;
                    Long value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        String partner = left.equals(productId.toString()) ? right : left;
                        coPurchaseMap.merge(partner, value, Long::sum);
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ [rec] Redis co-purchase fetch 실패: {}", e.getMessage());
        }

        // 구매 횟수 내림차순 정렬
        return coPurchaseMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());
    }


    public Page<ProductsResDto> getProducts(Pageable pageable) {
        return productRepository.findAllByOrderByViewCountDesc(pageable)
                .map(ProductsResDto::makeDto);
    }
}