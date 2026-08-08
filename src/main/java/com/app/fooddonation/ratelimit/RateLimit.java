package com.app.fooddonation.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller handler (or class) as rate limited. The {@link RateLimitInterceptor}
 * enforces a token-bucket per client key.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Namespace used to isolate buckets (e.g. "ai", "auth").
     */
    String bucket();

    /**
     * Maximum number of requests allowed per {@link #refillPeriodSeconds()}.
     */
    int capacity();

    /**
     * Refill period in seconds over which tokens are replenished.
     */
    int refillPeriodSeconds() default 60;
}
