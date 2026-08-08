package com.app.fooddonation.security;

import com.app.fooddonation.ratelimit.BucketStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Brute-force protection for the form login. Each (IP, username) pair gets a
 * token bucket; when it empties the account is locked for the refill window.
 * The bucket is shared via Redis so the lock holds across app instances.
 */
@Service
public class LoginAttemptService {

    private final BucketStore bucketStore;
    private final int maxAttempts;
    private final Duration refill;

    public LoginAttemptService(BucketStore bucketStore,
                               @Value("${app.security.login-lockout.max-attempts:5}") int maxAttempts,
                               @Value("${app.security.login-lockout.refill-minutes:5}") long refillMinutes) {
        this.bucketStore = bucketStore;
        this.maxAttempts = maxAttempts;
        this.refill = Duration.ofMinutes(refillMinutes);
    }

    public boolean isLocked(String username, String ip) {
        if (username == null) {
            return false;
        }
        return bucketStore.availableTokens(key(username, ip), maxAttempts, refill) == 0;
    }

    public void recordFailure(String username, String ip) {
        if (username == null) {
            return;
        }
        bucketStore.tryConsume(key(username, ip), maxAttempts, refill);
    }

    public void onSuccess(String username, String ip) {
        if (username == null) {
            return;
        }
        bucketStore.reset(key(username, ip));
    }

    private String key(String username, String ip) {
        return "login-attempt:" + ip + ":" + username.toLowerCase().trim();
    }
}
