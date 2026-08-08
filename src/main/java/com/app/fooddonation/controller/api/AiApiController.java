package com.app.fooddonation.controller.api;

import com.app.fooddonation.dto.AiListingRequest;
import com.app.fooddonation.dto.ChatRequest;
import com.app.fooddonation.dto.MessageResponse;
import com.app.fooddonation.ratelimit.RateLimit;
import com.app.fooddonation.service.AiService;
import com.app.fooddonation.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI-powered endpoints. Each call consumes from a shared rate-limit bucket
 * per user to protect the free-tier Groq quota.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RateLimit(bucket = "ai", capacity = 10, refillPeriodSeconds = 60)
@Tag(name = "AI", description = "Groq-powered listing writer, assistant and impact report")
public class AiApiController {

    private final AiService aiService;
    private final StatsService statsService;

    public AiApiController(AiService aiService, StatsService statsService) {
        this.aiService = aiService;
        this.statsService = statsService;
    }

    @PostMapping("/listing")
    @Operation(summary = "Generate a donation listing description with AI")
    public ResponseEntity<Map<String, String>> generateListing(@RequestBody AiListingRequest request) {
        return ResponseEntity.ok(aiService.generateListing(
                request.getFoodType(), request.getQuantity(), request.getCity(), request.getNotes()));
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with FoodBot (contextual platform assistant)")
    public ResponseEntity<MessageResponse> chat(@RequestBody ChatRequest request) {
        String reply = aiService.chat(request.getMessage(), statsService.getPlatformStats());
        return ResponseEntity.ok(MessageResponse.of(reply));
    }

    @PostMapping("/impact-report")
    @Operation(summary = "Generate an AI-written impact narrative from live metrics")
    public ResponseEntity<MessageResponse> impactReport() {
        String report = aiService.generateImpactReport(statsService.getPlatformStats());
        return ResponseEntity.ok(MessageResponse.of(report));
    }
}
