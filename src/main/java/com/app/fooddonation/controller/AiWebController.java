package com.app.fooddonation.controller;

import com.app.fooddonation.dto.AiListingRequest;
import com.app.fooddonation.dto.ChatRequest;
import com.app.fooddonation.dto.MessageResponse;
import com.app.fooddonation.ratelimit.RateLimit;
import com.app.fooddonation.service.AiService;
import com.app.fooddonation.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Server-rendered AI endpoints for the web UI (used by the donor form,
 * the FoodBot chat widget and the impact page). Authenticated via the web
 * session chain and rate limited per logged-in user, same as the REST API.
 */
@RestController
@RequestMapping("/ai")
@RateLimit(bucket = "ai", capacity = 10, refillPeriodSeconds = 60)
public class AiWebController {

    @Autowired
    private AiService aiService;

    @Autowired
    private StatsService statsService;

    @PostMapping("/listing")
    public Map<String, String> generateListing(@RequestBody AiListingRequest request) {
        return aiService.generateListing(
                request.getFoodType(), request.getQuantity(), request.getCity(), request.getNotes());
    }

    @PostMapping("/chat")
    public MessageResponse chat(@RequestBody ChatRequest request) {
        return MessageResponse.of(aiService.chat(request.getMessage(), statsService.getPlatformStats()));
    }

    @PostMapping("/impact-report")
    public MessageResponse impactReport() {
        return MessageResponse.of(aiService.generateImpactReport(statsService.getPlatformStats()));
    }
}
