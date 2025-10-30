package com.careup.branch.domain.recomendation.consume;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DebeziumOrderConsumer {

    @KafkaListener(topics = "careupdb.careup.orders", groupId = "careup-consumer")
    public void consume(ConsumerRecord<String, String> record) {
        System.out.println("✅ CDC 이벤트 수신됨 ===========================");
        System.out.println("Topic: " + record.topic());
        System.out.println("Key: " + record.key());
        System.out.println("Value: " + record.value());
        System.out.println("============================================");
    }
}