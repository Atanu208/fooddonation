package com.app.fooddonation.ratelimit;

import com.app.fooddonation.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Enforces per-client token-bucket rate limits using {@link BucketStore}.
 * Each client is keyed by authenticated principal (or IP for anonymous
 * requests) and bucket name, giving every user an isolated quota. The store
 * is either local (Bucket4j) or shared across instances (Redis).
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private final BucketStore bucketStore;
    private final boolean enabled;

    public RateLimitInterceptor(BucketStore bucketStore, boolean enabled) {
        this.bucketStore = bucketStore;
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        if (!enabled) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            rateLimit = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }
        if (rateLimit == null) {
            return true;
        }

        String clientKey = resolveClientKey(request);
        final RateLimit limit = rateLimit;
        String bucketKey = limit.bucket() + ":" + clientKey;

        if (!bucketStore.tryConsume(bucketKey, limit.capacity(),
                Duration.ofSeconds(limit.refillPeriodSeconds()))) {
            response.setHeader("Retry-After", String.valueOf(limit.refillPeriodSeconds()));
            throw new RateLimitExceededException("Rate limit exceeded for '" + limit.bucket()
                    + "'. Please retry after " + limit.refillPeriodSeconds() + " seconds.");
        }
        return true;
    }

    private String resolveClientKey(HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            return "user:" + request.getUserPrincipal().getName();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
