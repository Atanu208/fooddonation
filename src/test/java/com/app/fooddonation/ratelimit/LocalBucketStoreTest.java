package com.app.fooddonation.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the token-bucket store used by the rate limiter and the
 * login brute-force lockout.
 */
class LocalBucketStoreTest {

    private final LocalBucketStore store = new LocalBucketStore();
    private final Duration refill = Duration.ofSeconds(60);

    @Test
    @DisplayName("allows capacity tokens then refuses excess")
    void capacityIsEnforced() {
        String key = "ai:user:test";

        assertThat(store.tryConsume(key, 3, refill)).isTrue();
        assertThat(store.tryConsume(key, 3, refill)).isTrue();
        assertThat(store.tryConsume(key, 3, refill)).isTrue();
        assertThat(store.tryConsume(key, 3, refill)).isFalse();
    }

    @Test
    @DisplayName("keys are isolated from each other")
    void keysAreIsolated() {
        assertThat(store.tryConsume("a", 1, refill)).isTrue();
        assertThat(store.tryConsume("a", 1, refill)).isFalse();
        assertThat(store.tryConsume("b", 1, refill)).isTrue();
    }

    @Test
    @DisplayName("unknown keys report a full bucket")
    void unknownKeyIsFull() {
        assertThat(store.availableTokens("missing", 5, refill)).isEqualTo(5);
    }

    @Test
    @DisplayName("reset restores the full quota")
    void resetRestoresQuota() {
        String key = "login:127.0.0.1:test@demo.com";
        assertThat(store.tryConsume(key, 2, refill)).isTrue();
        assertThat(store.tryConsume(key, 2, refill)).isTrue();
        assertThat(store.availableTokens(key, 2, refill)).isZero();

        store.reset(key);
        assertThat(store.availableTokens(key, 2, refill)).isEqualTo(2);
    }
}
