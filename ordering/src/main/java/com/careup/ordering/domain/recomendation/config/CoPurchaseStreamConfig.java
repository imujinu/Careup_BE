package com.careup.ordering.domain.recomendation.config;


import com.careup.ordering.domain.recomendation.dto.OrderedItemEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableKafkaStreams
@RequiredArgsConstructor
@Slf4j
public class CoPurchaseStreamConfig {

    private final ObjectMapper objectMapper;
    @Qualifier("coPurchaseRedisTemplate")
    private final RedisTemplate<String, Long> redisTemplate; // ✅ Redis 주입

    @Bean
    public KStream<String, String> coPurchaseStream(StreamsBuilder builder) {
        log.info("[rec][coPurchase] : 함께 구매된 횟수 카운팅 시작");

        // ✅ 1️⃣ Debezium 토픽(JSON String)을 수신
        KStream<String, String> orderedItemStream = builder.stream(
                "careupdb.careup.ordered_item",
                Consumed.with(Serdes.String(),
                        Serdes.String(),
                        new DebeziumTimestampExtractor(),
                        Topology.AutoOffsetReset.EARLIEST
                )
        );

        // ✅ 2️⃣ Debezium 메시지(JSON) → OrderedItemEvent 로 변환
        KStream<String, OrderedItemEvent> parsedStream = orderedItemStream
                .mapValues(value -> {
                    try {
                        JsonNode root = objectMapper.readTree(value);
                        JsonNode after = root.path("payload").path("after");
                        if (after.isMissingNode() || after.isNull()) return null;
                        return objectMapper.treeToValue(after, OrderedItemEvent.class);
                    } catch (Exception e) {
                        log.error("❌ [rec] JSON 파싱 실패: {}", e.getMessage());
                        return null;
                    }
                })
                .filter((k, v) -> v != null)
                .peek((k, v) -> log.info("✅ [rec] OrderedItemEvent 수신됨: {}", v));

        // ✅ 3️⃣ orderId 기준으로 상품쌍 집계 후 Redis에 저장
        parsedStream
                .filter((k, v) -> v.getOrderId() != null && v.getBranchProductId() != null)
                .groupBy(
                        (key, value) -> value.getOrderId().toString(),
                        Grouped.with(Serdes.String(), new JsonSerde<>(OrderedItemEvent.class))
                )
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(1)))
                .aggregate(
                        ArrayList::new,
                        (orderId, newItem, aggregate) -> {
                            aggregate.add(newItem.getBranchProductId());
                            return aggregate;
                        },
                        Materialized.with(Serdes.String(), new JsonSerde<>(ArrayList.class))
                )
                .toStream()
                .map((windowedKey, items) -> KeyValue.pair(windowedKey.key(), items))
                .flatMap((orderId, items) -> {
                    List<KeyValue<String, String>> pairs = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        for (int j = i + 1; j < items.size(); j++) {
                            String pairKey = items.get(i) + "-" + items.get(j);
                            pairs.add(KeyValue.pair(pairKey, "1"));
                        }
                    }
                    return pairs;
                })
                .foreach((pairKey, one) -> {
                    try {
                        // ✅ Redis에서 pairKey 기준으로 카운트 증가 (atomic)
                        Long newCount = redisTemplate.opsForValue().increment("co_purchase_count:" + pairKey, 1);
                        log.info("🟢 [rec][Redis] 상품쌍={} | 누적횟수={}", pairKey, newCount);
                    } catch (Exception e) {
                        log.error("❌ [rec][Redis] INCR 실패: {}", e.getMessage());
                    }
                });

        log.info("[rec] Kafka Streams → Redis 파이프라인 실행 완료 ✅");
        return orderedItemStream;
    }

    // ✅ Debezium 이벤트의 ts_ms를 타임스탬프로 사용
    public static class DebeziumTimestampExtractor implements TimestampExtractor {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
            try {
                JsonNode node = mapper.readTree(record.value().toString());
                return node.path("payload").path("source").path("ts_ms").asLong();
            } catch (Exception e) {
                return partitionTime;
            }
        }
    }
}

