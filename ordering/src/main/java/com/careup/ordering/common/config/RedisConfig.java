package com.careup.ordering.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;               // ★ 추가
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

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration();
        conf.setHostName(host);
        conf.setPort(port);
        conf.setDatabase(0); // 재고관리용 db
        return new LettuceConnectionFactory(conf);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setKeySerializer(new StringRedisSerializer());
        t.setHashKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        t.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        t.setConnectionFactory(cf); // @Primary가 주입됨
        return t;
    }

    @Bean(name = "rtInventoryConnectionFactory")
    public RedisConnectionFactory rtInventoryConnectionFactory(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") int port
    ) {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration();
        conf.setHostName(host);
        conf.setPort(port);
        conf.setDatabase(1);
        return new LettuceConnectionFactory(conf);
    }

    @Bean(name = "rtInventory")
    public RedisTemplate<String, String> rtInventoryRedisTemplate(
            @Qualifier("rtInventoryConnectionFactory") RedisConnectionFactory cf
    ) {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        t.setConnectionFactory(cf);
        return t;
    }

    @Bean(name = "coPurchaseConnectionFactory")
    public RedisConnectionFactory coPurchaseConnectionFactory(
            @Value("${spring.redis.host}") String host,
            @Value("${spring.redis.port}") int port
    ) {
        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration();
        conf.setHostName(host);
        conf.setPort(port);
        conf.setDatabase(3); // ✅ co-purchase용 DB index
        return new LettuceConnectionFactory(conf);
    }

    @Bean(name = "coPurchaseRedisTemplate")
    public RedisTemplate<String, Long> coPurchaseRedisTemplate(
            @Qualifier("coPurchaseConnectionFactory") RedisConnectionFactory cf
    ) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setConnectionFactory(cf);
        return template;
    }
}
