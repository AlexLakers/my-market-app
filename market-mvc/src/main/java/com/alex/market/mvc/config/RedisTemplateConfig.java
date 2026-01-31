package com.alex.market.mvc.config;

import com.alex.market.mvc.cache.ItemCache;
import com.alex.market.mvc.cache.PageInfoCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisTemplateConfig {


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(ItemCache.class));

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(ItemCache.class));

        return redisTemplate;
    }

    @Bean
    public ReactiveRedisTemplate<String, ItemCache> itemCacheReactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        RedisSerializationContext<String, ItemCache> serializationContext =
                RedisSerializationContext.<String, ItemCache>newSerializationContext()
                        .key(StringRedisSerializer.UTF_8)
                        .value(new Jackson2JsonRedisSerializer<>(ItemCache.class))
                        .hashKey(StringRedisSerializer.UTF_8)
                        .hashValue(new Jackson2JsonRedisSerializer<>(ItemCache.class))
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
    @Bean
    public ReactiveRedisTemplate<String, PageInfoCache> pageInfoCacheReactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        RedisSerializationContext<String, PageInfoCache> serializationContext =
                RedisSerializationContext.<String, PageInfoCache>newSerializationContext()
                        .key(StringRedisSerializer.UTF_8)
                        .value(new Jackson2JsonRedisSerializer<>(PageInfoCache.class))
                        .hashKey(StringRedisSerializer.UTF_8)
                        .hashValue(new Jackson2JsonRedisSerializer<>(PageInfoCache.class))
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}
