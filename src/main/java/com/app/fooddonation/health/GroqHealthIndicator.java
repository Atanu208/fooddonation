package com.app.fooddonation.health;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Surfaces the Groq AI integration health to Actuator based on the resilience
 * circuit state: open/half-open => DOWN, closed => UP.
 */
@Component
public class GroqHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public GroqHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public Health health() {
        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("groq");
        return switch (breaker.getState()) {
            case CLOSED -> Health.up()
                    .withDetail("state", breaker.getState().name())
                    .withDetail("failureRate", breaker.getMetrics().getFailureRate())
                    .build();
            default -> Health.down()
                    .withDetail("state", breaker.getState().name())
                    .withDetail("failureRate", breaker.getMetrics().getFailureRate())
                    .build();
        };
    }
}
