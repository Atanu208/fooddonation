package com.app.fooddonation.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

/**
 * Distributed token-bucket store backed by Redis (via a small Lua script run
 * atomically). Every app instance shares the same quotas, so rate limits and
 * brute-force locks stay correct across a horizontally-scaled deployment.
 */
public class RedisBucketStore implements BucketStore {

    private static final Logger log = LoggerFactory.getLogger(RedisBucketStore.class);

    private static final String CONSUME_SCRIPT = """
            local capacity = tonumber(ARGV[1])
            local refillPerSec = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local key = KEYS[1]
            local v = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(v[1])
            local ts = tonumber(v[2])
            if tokens == nil then tokens = capacity end
            if ts == nil then ts = now end
            tokens = tokens + (now - ts) * refillPerSec
            if tokens > capacity then tokens = capacity end
            if tokens < 1 then
                redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
                redis.call('EXPIRE', key, math.floor(capacity / refillPerSec) + 2)
                return 0
            end
            tokens = tokens - 1
            redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
            redis.call('EXPIRE', key, math.floor(capacity / refillPerSec) + 2)
            return 1
            """;

    private static final String PEEK_SCRIPT = """
            local capacity = tonumber(ARGV[1])
            local refillPerSec = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local key = KEYS[1]
            local v = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(v[1])
            local ts = tonumber(v[2])
            if tokens == nil then return capacity end
            if ts == nil then ts = now end
            tokens = tokens + (now - ts) * refillPerSec
            if tokens > capacity then tokens = capacity end
            return math.floor(tokens)
            """;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> consumeScript;
    private final DefaultRedisScript<Long> peekScript;

    public RedisBucketStore(StringRedisTemplate redis) {
        this.redis = redis;
        this.consumeScript = new DefaultRedisScript<>(CONSUME_SCRIPT, Long.class);
        this.peekScript = new DefaultRedisScript<>(PEEK_SCRIPT, Long.class);
    }

    @Override
    public boolean tryConsume(String key, int capacity, Duration refillPeriod) {
        try {
            Long result = redis.execute(consumeScript, List.of(key),
                    String.valueOf(capacity),
                    String.valueOf((double) capacity / refillPeriod.toSeconds()),
                    String.valueOf(System.currentTimeMillis() / 1000.0));
            return result != null && result == 1L;
        } catch (Exception ex) {
            log.warn("Redis rate-limit bucket unavailable for key '{}'; denying? {} (fail-open=false)",
                    key, ex.getMessage());
            return false;
        }
    }

    @Override
    public long availableTokens(String key, int capacity, Duration refillPeriod) {
        try {
            Long result = redis.execute(peekScript, List.of(key),
                    String.valueOf(capacity),
                    String.valueOf((double) capacity / refillPeriod.toSeconds()),
                    String.valueOf(System.currentTimeMillis() / 1000.0));
            // The Lua script already floors, so a partially refilled (< 1 token)
            // bucket still counts as empty.
            return result == null ? capacity : result;
        } catch (Exception ex) {
            log.warn("Redis rate-limit bucket peek failed for key '{}': {}", key, ex.getMessage());
            return capacity;
        }
    }

    @Override
    public void reset(String key) {
        try {
            redis.delete(key);
        } catch (Exception ex) {
            log.warn("Failed to reset rate-limit bucket '{}': {}", key, ex.getMessage());
        }
    }
}
