package com.app.fooddonation.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin, resilient HTTP client for the Groq LLM API. Protected by a circuit
 * breaker and retry policy so a degraded AI provider never takes down the
 * core platform. Observability metrics are exposed via Micrometer/Prometheus.
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Timer requestTimer;

    public GroqClient(RestTemplate restTemplate,
                      ObjectMapper objectMapper,
                      @Value("${groq.api.key}") String apiKey,
                      @Value("${groq.api.model}") String model,
                      @Value("${groq.api.base-url}") String baseUrl,
                      MeterRegistry meterRegistry) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.successCounter = Counter.builder("groq.requests.success").register(meterRegistry);
        this.failureCounter = Counter.builder("groq.requests.failure").register(meterRegistry);
        this.requestTimer = Timer.builder("groq.requests.duration").register(meterRegistry);
    }

    @CircuitBreaker(name = "groq", fallbackMethod = "completeFallback")
    @Retry(name = "groq")
    public String complete(String systemPrompt, String userPrompt, double temperature) {
        return requestTimer.record(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("temperature", temperature);
            body.put("max_tokens", 700);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            try {
                ResponseEntity<JsonNode> response = restTemplate.exchange(
                        baseUrl + "/chat/completions", HttpMethod.POST, request, JsonNode.class);

                JsonNode bodyNode = response.getBody();
                if (bodyNode == null || !bodyNode.has("choices") || bodyNode.path("choices").isEmpty()) {
                    failureCounter.increment();
                    throw new IllegalStateException("No AI completion returned");
                }
                successCounter.increment();
                return bodyNode.path("choices").get(0).path("message").path("content").asText("").trim();
            } catch (Exception ex) {
                failureCounter.increment();
                throw ex;
            }
        });
    }

    /**
     * Graceful degradation used when the circuit is open or retries are
     * exhausted. Returns a polite message instead of failing the request.
     */
    @SuppressWarnings("unused")
    private String completeFallback(String systemPrompt, String userPrompt, double temperature, Throwable t) {
        log.warn("Groq request failed, serving fallback: {}", t.getMessage());
        return "I'm sorry, the AI service is temporarily unavailable. "
                + "Please try again in a few minutes.";
    }
}
