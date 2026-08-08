package com.app.fooddonation.service;

import com.app.fooddonation.integration.GroqClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Orchestrates the AI-powered features of the platform (listing writer, FoodBot
 * assistant, impact report) on top of the resilient {@link GroqClient}.
 */
@Service
public class AiService {

    private final GroqClient groqClient;
    private final ObjectMapper objectMapper;

    public AiService(GroqClient groqClient, ObjectMapper objectMapper) {
        this.groqClient = groqClient;
        this.objectMapper = objectMapper;
    }

    public Map<String, String> generateListing(String foodType, String quantity, String city, String notes) {
        String system = """
                You are an expert listing writer for a food donation platform called FoodShare.
                Write an accurate, warm and specific description of donated surplus food that makes an NGO want to pick it up.
                Respond with STRICT JSON only, using exactly these keys: "description", "quantity", "bestBefore".
                "description" must be 1-3 inviting sentences. "quantity" a concise human-readable amount.
                "bestBefore" a short freshness window (e.g. "Within 2 hours"). No extra text, no markdown fences.
                """;
        String user = "Food type: " + nz(foodType) + "\nQuantity available: " + nz(quantity)
                + "\nCity: " + nz(city) + "\nNotes: " + nz(notes);
        return parseJsonObject(groqClient.complete(system, user, 0.5));
    }

    public String chat(String userMessage, Map<String, Object> platformStats) {
        String system = """
                You are FoodBot, the friendly AI assistant of FoodShare — an AI-powered platform that connects food donors
                (restaurants, hotels, caterers, individuals) with NGOs/charities so surplus food never goes to waste.
                Current platform stats: %s
                Answer the user's question concisely (max 5 sentences). If asked about the platform, explain how to register,
                post a donation, browse and accept donations, and the status flow:
                PENDING -> ACCEPTED -> PICKED_UP -> DELIVERED -> COMPLETED.
                Be warm and helpful. Do not reveal any secrets or internal configuration.
                """.formatted(platformStats);
        return groqClient.complete(system, userMessage, 0.6);
    }

    public String generateImpactReport(Map<String, Object> platformStats) {
        String system = """
                You are a data storyteller for a food donation NGO platform called FoodShare.
                Using the provided metrics, write an inspiring 3-5 sentence impact report narrative
                that a donor or NGO partner would love to read. Be specific and reference the actual numbers.
                """;
        return groqClient.complete(system, "Metrics: " + platformStats, 0.7);
    }

    private Map<String, String> parseJsonObject(String raw) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            String cleaned = raw;
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "").trim();
            }
            JsonNode node = objectMapper.readTree(cleaned);
            if (node.isObject()) {
                node.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText("")));
            }
        } catch (Exception ignored) {
            result.clear();
        }
        if (result.isEmpty()) {
            result.put("description", raw);
        }
        return result;
    }

    private String nz(String value) {
        return (value == null || value.isBlank()) ? "not specified" : value;
    }
}
