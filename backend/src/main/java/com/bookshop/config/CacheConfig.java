package com.bookshop.config;

import com.bookshop.book.BookDto;
import com.bookshop.common.PageResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

/**
 * Redis-backed read caching for the catalog (the hottest, most cache-friendly
 * endpoint). Two design points worth noting:
 *
 * <ul>
 *   <li><b>Typed JSON serializers per cache</b> - values are stored as plain
 *       JSON and deserialized back into their exact record types, avoiding the
 *       classic LinkedHashMap-on-cache-hit pitfall of generic serializers.</li>
 *   <li><b>Graceful degradation</b> - the {@link CacheErrorHandler} logs and
 *       swallows Redis connectivity errors, so if Redis is down or absent the
 *       app silently serves straight from PostgreSQL instead of failing.</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    // The version suffix is bumped whenever the cached DTO shape changes, so a
    // deploy never serves entries written against the previous structure.
    /** Paginated catalog responses, keyed by page/size/search/sort. Short TTL. */
    public static final String BOOKS_PAGE_CACHE = "booksPage.v2";
    /** Single book lookups, keyed by id. */
    public static final String BOOK_CACHE = "book.v2";

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheCustomizer(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper.copy();
        JavaType pageType = mapper.getTypeFactory()
                .constructParametricType(PageResponse.class, BookDto.class);
        return builder -> builder
                .withCacheConfiguration(BOOKS_PAGE_CACHE, cacheConfig(
                        Duration.ofSeconds(60),
                        new Jackson2JsonRedisSerializer<PageResponse<BookDto>>(mapper, pageType)))
                .withCacheConfiguration(BOOK_CACHE, cacheConfig(
                        Duration.ofMinutes(5),
                        new Jackson2JsonRedisSerializer<>(mapper, BookDto.class)));
    }

    @SuppressWarnings("unchecked")
    private static RedisCacheConfiguration cacheConfig(Duration ttl, RedisSerializer<?> serializer) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(SerializationPair.fromSerializer((RedisSerializer<Object>) serializer));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET failed on '{}' - serving from database: {}", cache.getName(),
                        exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key,
                                            Object value) {
                log.warn("Cache PUT failed on '{}': {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT failed on '{}': {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR failed on '{}': {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
