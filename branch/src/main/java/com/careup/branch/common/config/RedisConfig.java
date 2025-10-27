package com.careup.branch.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.port}")
    private int port;

    /// 0) 기본(Primary) 커넥션 팩토리  → DB 0
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration();
        conf.setHostName(host);
        conf.setPort(port);
        conf.setDatabase(0);
        return new LettuceConnectionFactory(conf);
    }

    /// 1) 기본(Primary) redisTemplate
    @Bean(name = "redisTemplate")
    @Primary
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> t = new RedisTemplate<>();
        t.setKeySerializer(new StringRedisSerializer());
        t.setHashKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        t.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        t.setConnectionFactory(redisConnectionFactory);
        return t;
    }

    /// 2) SSE Pub/Sub용 커넥션/템플릿 → DB 1
    @Bean
    @Qualifier("ssePubSub")
    public RedisConnectionFactory sseFactory() {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(host);
        configuration.setPort(port);
        configuration.setDatabase(1);
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    @Qualifier("ssePubSub")
    public RedisTemplate<String, String> sseRedisTemplate(
            @Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory
    ) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    /// 3) Refresh Token 전용 커넥션/템플릿 → DB 2
    @Bean
    @Qualifier("rtInventory")
    public RedisConnectionFactory rtFactory() {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration();
        conf.setHostName(host);
        conf.setPort(port);
        conf.setDatabase(2);
        return new LettuceConnectionFactory(conf);
    }

    @Bean
    @Qualifier("rtInventory")
    public RedisTemplate<String, String> rtRedisTemplate(
            @Qualifier("rtInventory") RedisConnectionFactory cf
    ) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        t.setConnectionFactory(cf);
        return t;
    }

//    // redis 리스너 객체
//    @Bean
//    @Qualifier("ssePubSub")
//    public RedisMessageListenerContainer redisMessageListenerContainer(
//            @Qualifier("ssePubSub") RedisConnectionFactory redisConnectionFactory,
//            @Qualifier("sseMessageListenerAdapter") MessageListenerAdapter messageListenerAdapter
//    ) {
//        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//        container.setConnectionFactory(redisConnectionFactory);
//        container.addMessageListener(messageListenerAdapter, new PatternTopic("notification-channel"));
//        return container;
//    }
//
//    // redis의 채널에서 수신된 메시지를 처리하는 빈객체 (위에서 수신한걸 여기서 처리한다.)
//    @Bean
//    @Qualifier("sseMessageListenerAdapter")
//    // TODO: 실제 서비스 타입으로 교체할 것
//    public MessageListenerAdapter messageListenerAdapter(String sseAlarmService) {
//        return new MessageListenerAdapter(sseAlarmService, "onMessage");
//    }
}
