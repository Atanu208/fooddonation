package com.app.fooddonation.ratelimit;

import java.time.Duration;

/**
 * Abstraction over the token-bucket storage used for rate limiting.
 * <p>
 * A single instance uses {@link LocalBucketStore} (in-memory, Bucket4j);
 * a horizontally-scaled deployment uses {@link RedisBucketStore} so all app
 * instances share one quota per client.
 */
public interface BucketStore {

    /**
     * Atomically tries to consume one token from the bucket.
     *
     * @param key           unique bucket identifier (client + quota name)
     * @param capacity      maximum tokens the bucket holds
     * @param refillPeriod  how long it takes for the bucket to fully refill
     * @return {@code true} if a token was available and consumed
     */
    boolean tryConsume(String key, int capacity, Duration refillPeriod);

    /**
     * @return the number of tokens currently available (a full bucket if the
     *         key is unknown)
     */
    long availableTokens(String key, int capacity, Duration refillPeriod);

    /**
     * Removes the bucket, resetting the quota for the given key.
     */
    void reset(String key);
}
