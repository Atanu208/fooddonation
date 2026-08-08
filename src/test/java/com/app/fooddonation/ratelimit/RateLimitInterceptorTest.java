package com.app.fooddonation.ratelimit;

import com.app.fooddonation.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the token-bucket rate limiter: per-client quotas and the
 * disabled switch used in tests.
 */
class RateLimitInterceptorTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private HandlerMethod handler;

    @BeforeEach
    void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        handler = new HandlerMethod(new SampleController(), SampleController.class.getMethod("limited"));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    @DisplayName("enforces the annotated capacity per client then rejects excess")
    void enforcesCapacity() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(new LocalBucketStore(), true);

        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();

        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(RateLimitExceededException.class);

        verify(response).setHeader("Retry-After", "60");
    }

    @Test
    @DisplayName("keys buckets by authenticated principal instead of IP")
    void keysBucketsByPrincipal() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(new LocalBucketStore(), true);
        when(request.getUserPrincipal()).thenReturn(() -> "donor@demo.com");

        // Different principals never share a bucket even from the same IP.
        when(request.getRemoteAddr()).thenReturn("203.0.113.7");
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();

        when(request.getUserPrincipal()).thenReturn(() -> "ngo@demo.com");
        assertThat(interceptor.preHandle(request, response, handler)).isTrue();
    }

    @Test
    @DisplayName("passes everything through when disabled")
    void disabled_allowsAll() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(new LocalBucketStore(), false);

        for (int i = 0; i < 10; i++) {
            assertThat(interceptor.preHandle(request, response, handler)).isTrue();
        }
    }

    @Test
    @DisplayName("ignores handlers without the @RateLimit annotation")
    void unannotatedHandlersAreAllowed() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(new LocalBucketStore(), true);
        HandlerMethod plain = new HandlerMethod(new SampleController(), SampleController.class.getMethod("plain"));

        assertThat(interceptor.preHandle(request, response, plain)).isTrue();
    }

    static class SampleController {
        @RateLimit(bucket = "ai", capacity = 2, refillPeriodSeconds = 60)
        public void limited() {
        }

        public void plain() {
        }
    }
}
