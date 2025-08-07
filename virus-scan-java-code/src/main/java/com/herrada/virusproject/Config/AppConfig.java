package com.herrada.virusproject.Config;

import com.herrada.virusproject.ClamAV.ClamavClient;
import com.herrada.virusproject.ClamAV.ScanRequest;
import com.herrada.virusproject.ClamAV.ScanResultInfo;
import com.herrada.virusproject.Keys.CustomKeyGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;


@Configuration
public class AppConfig {

    @Bean
    public  RedisTemplate<String, ScanRequest> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        Jackson2JsonRedisSerializer<ScanRequest> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(ScanRequest.class);

        RedisTemplate<String, ScanRequest> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(jsonSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {

        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory);
        stringRedisTemplate.setKeySerializer(new StringRedisSerializer());
        stringRedisTemplate.setValueSerializer(new StringRedisSerializer());
        return stringRedisTemplate;
    }

    @Bean
    public RedisTemplate<String, ScanResultInfo> jsonRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ScanResultInfo> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use Jackson for JSON serialization
        Jackson2JsonRedisSerializer<ScanResultInfo> jsonSerializer = new Jackson2JsonRedisSerializer<>(ScanResultInfo.class);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        return template;
    }

    @Bean
    ClamavClient clamavClient(@Value("${application.socket-path}") String path) {
        return new ClamavClient(path);
    }

    @Bean
    @Profile({"prod","docker","minikube"})
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Jackson2JsonRedisSerializer<ScanResultInfo> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(ScanResultInfo.class);
        RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5L))
                .disableKeyPrefix()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    @Bean("customKeyGenerator")
    public CustomKeyGenerator customKeyGenerator() throws NoSuchAlgorithmException {
        return new CustomKeyGenerator();
    }

    /*
    Using Java-based caching libs when running unit tests.
     Not required to use redis cache for this test.
     */
    @Bean
    @Profile("test")
    public SimpleCacheManager simpleCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new ConcurrentMapCache("scanCache")));
        return cacheManager;
    }

}
