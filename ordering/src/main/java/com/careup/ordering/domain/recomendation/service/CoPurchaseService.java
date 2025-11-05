package com.careup.ordering.domain.recomendation.service;

import com.careup.ordering.domain.member.service.MemberQueryService;
import com.careup.ordering.domain.order.dto.response.ProductViewCountResDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.repository.ProductRepository;
import com.careup.ordering.domain.recomendation.dto.ProductCoPurchaseResDto;
import com.careup.ordering.domain.recomendation.dto.ProductWithScore;
import com.careup.ordering.domain.recomendation.dto.ProductWithSimilarity;
import com.careup.ordering.domain.recomendation.dto.ProductsResDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public void increaseCoPurchase(Long productAId, Long productBId) {
        // 항상 작은 ID가 A가 되도록 정렬
        Long a = Math.min(productAId, productBId);
        Long b = Math.max(productAId, productBId);

        ProductCoPurchase co = coPurchaseRepository
                .findByProductAIdAndProductBId(a, b)
                .orElseGet(() -> ProductCoPurchase.builder()
                        .productAId(a)
                        .productBId(b)
                        .coPurchaseCount(0L)
                        .build());

        co.increaseCount();
        coPurchaseRepository.save(co);
    }



    public ProductCoPurchaseResDto getCoPurchaseList(Long memberId, Pageable pageable) {
        ProductViewCountResDto dto = memberQueryService.getProductId(memberId);
        Long productId = dto.getProductId();
        Product lastViewProduct = productRepository.findById(productId).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 상품입니다."));
        List<ProductWithSimilarity> similarProducts = qdrantService.searchSimilarProducts(productId, 50);
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
                .hasRecentView(dto.isHasRecentView())
                .lastViewProductName(lastViewProduct.getName())
                .build();
    }



    public List<Map.Entry<String, Long>> getCoPurchasedProducts(Long productId) {
        KafkaStreams streams = factoryBean.getKafkaStreams();
        if (streams == null) throw new IllegalStateException("Kafka Streams is not ready yet");



        ReadOnlyWindowStore<String, Long> store =
                streams.store(StoreQueryParameters.fromNameAndType(
                        "co_purchase_count",
                        QueryableStoreTypes.windowStore()
                ));
        ZoneId zone = ZoneId.of("Asia/Seoul");
        Instant now = ZonedDateTime.now(zone).toInstant();
        Instant from = now.minus(Duration.ofDays(30));
        Instant to = now;
        Map<String, Long> coPurchaseMap = new HashMap<>();
        // 전체 스토어 순회
        store.fetchAll(from, to).forEachRemaining(record -> {
            Windowed<String> windowedKey = record.key;
            String pairKey = windowedKey.key(); // 예: "1-2"
            Long count = record.value;


            String[] parts = pairKey.split("-");
            if (parts.length != 2) return;

            String left = parts[0];
            String right = parts[1];

            // 특정 productId가 들어있는 쌍만 필터링
            if (left.equals(productId.toString())) {
                coPurchaseMap.merge(right, count, Long::sum);
            } else if (right.equals(productId.toString())) {
                coPurchaseMap.merge(left, count, Long::sum);
            }
        });




        // 구매 횟수 높은 순으로 정렬
        return coPurchaseMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10) // 상위 10개
                .collect(Collectors.toList());
    }


    public List<ProductsResDto> getProducts(Pageable pageable) {
        return productRepository.findAll().stream().map(p -> ProductsResDto.makeDto(p)).collect(Collectors.toList());
    }
}