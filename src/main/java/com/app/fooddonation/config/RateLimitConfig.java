package com.app.fooddonation.config;

import com.app.fooddonation.ratelimit.BucketStore;
import com.app.fooddonation.ratelimit.LocalBucketStore;
import com.app.fooddonation.ratelimit.RateLimitInterceptor;
import com.app.fooddonation.ratelimit.RedisBucketStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Wiring for the rate-limiter store. Uses Redis when the app runs distributed
 * ({@code app.ratelimit.storage=redis}, the production default) and falls
 * back to an in-memory Bucket4j store for tests and single instances.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    @ConditionalOnProperty(name = "app.ratelimit.storage", havingValue = "redis", matchIfMissing = true)
    public BucketStore redisBucketStore(StringRedisTemplate redisTemplate) {
        return new RedisBucketStore(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(BucketStore.class)
    public BucketStore localBucketStore() {
        return new LocalBucketStore();
    }

    @Bean
    public RateLimitInterceptor rateLimitInterceptor(BucketStore bucketStore,
                                                     @Value("${app.ratelimit.enabled:true}") boolean enabled) {
        return new RateLimitInterceptor(bucketStore, enabled);
    }
}
