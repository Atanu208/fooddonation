package com.app.fooddonation.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token-bucket store backed by Bucket4j. Suitable for single
 * instances and tests; prefer {@link RedisBucketStore} when scaling out.
 */
public class LocalBucketStore implements BucketStore {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String key, int capacity, Duration refillPeriod) {
        return buckets.computeIfAbsent(key, k -> buildBucket(capacity, refillPeriod)).tryConsume(1);
    }

    @Override
    public long availableTokens(String key, int capacity, Duration refillPeriod) {
        Bucket bucket = buckets.get(key);
        return bucket == null ? capacity : bucket.getAvailableTokens();
    }

    @Override
    public void reset(String key) {
        buckets.remove(key);
    }

    private Bucket buildBucket(int capacity, Duration refillPeriod) {
        Bandwidth bandwidth = Bandwidth.classic(capacity,
                Refill.greedy(capacity, refillPeriod));
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
