package com.app.fooddonation.security;

import com.app.fooddonation.ratelimit.LocalBucketStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the brute-force login lockout using the token-bucket store.
 */
class LoginAttemptServiceTest {

    private final LoginAttemptService service =
            new LoginAttemptService(new LocalBucketStore(), 5, 5);

    @Test
    @DisplayName("locks after max attempts and unlocks after reset")
    void locksAfterMaxAttempts() {
        String ip = "10.0.0.5";
        String username = "donor@demo.com";

        assertThat(service.isLocked(username, ip)).isFalse();

        for (int i = 0; i < 5; i++) {
            service.recordFailure(username, ip);
        }
        assertThat(service.isLocked(username, ip)).isTrue();

        service.onSuccess(username, ip);
        assertThat(service.isLocked(username, ip)).isFalse();
    }

    @Test
    @DisplayName("locks are scoped per username so one user cannot block another")
    void lockIsPerUser() {
        String ip = "10.0.0.9";

        for (int i = 0; i < 6; i++) {
            service.recordFailure("target@demo.com", ip);
        }
        assertThat(service.isLocked("target@demo.com", ip)).isTrue();
        assertThat(service.isLocked("other@demo.com", ip)).isFalse();
    }

    @Test
    @DisplayName("null username never locks")
    void nullUsernameNeverLocks() {
        assertThat(service.isLocked(null, "1.2.3.4")).isFalse();
    }
}
