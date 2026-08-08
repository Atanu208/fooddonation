package com.app.fooddonation.service;

import com.app.fooddonation.integration.GroqClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the AI orchestration service: JSON parsing, markdown-fence
 * stripping and prompt construction.
 */
@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock
    private GroqClient groqClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiService aiService;

    @Test
    @DisplayName("generateListing parses the structured JSON returned by Groq")
    void generateListing_parsesJson() throws Exception {
        when(groqClient.complete(anyString(), anyString(), anyDouble()))
                .thenReturn("""
                        {"description":"Fresh hot biryani","quantity":"20 boxes","bestBefore":"Within 2 hours"}
                        """);
        when(objectMapper.readTree(anyString()))
                .thenReturn(new ObjectMapper().readTree(
                        "{\"description\":\"Fresh hot biryani\",\"quantity\":\"20 boxes\",\"bestBefore\":\"Within 2 hours\"}"));

        Map<String, String> result = aiService.generateListing("Vegetarian", "20 boxes", "Mumbai", null);

        assertThat(result).containsEntry("description", "Fresh hot biryani")
                .containsEntry("quantity", "20 boxes")
                .containsEntry("bestBefore", "Within 2 hours");
    }

    @Test
    @DisplayName("generateListing strips markdown code fences around the JSON")
    void generateListing_stripsMarkdownFences() throws Exception {
        when(groqClient.complete(anyString(), anyString(), anyDouble()))
                .thenReturn("```json\n{\"description\":\"Boxed rice\",\"quantity\":\"10 kg\"}\n```");
        when(objectMapper.readTree(anyString()))
                .thenReturn(new ObjectMapper().readTree("{\"description\":\"Boxed rice\",\"quantity\":\"10 kg\"}"));

        Map<String, String> result = aiService.generateListing("Vegetarian", "10 kg", "Delhi", null);

        assertThat(result).containsEntry("description", "Boxed rice")
                .containsEntry("quantity", "10 kg");
    }

    @Test
    @DisplayName("generateListing falls back to raw text when Groq returns no JSON")
    void generateListing_fallsBackToRawText() throws Exception {
        when(groqClient.complete(anyString(), anyString(), anyDouble()))
                .thenReturn("Just some free rice for pickup.");
        when(objectMapper.readTree(anyString()))
                .thenThrow(new com.fasterxml.jackson.databind.JsonMappingException((com.fasterxml.jackson.core.JsonParser) null, "not json"));

        Map<String, String> result = aiService.generateListing(null, null, null, null);

        assertThat(result).containsEntry("description", "Just some free rice for pickup.");
    }

    @Test
    @DisplayName("chat forwards the user message and injects FoodBot context")
    void chat_injectsContextAndReturnsReply() {
        when(groqClient.complete(anyString(), anyString(), anyDouble()))
                .thenReturn("Register, then post your donation.");

        String reply = aiService.chat("How do I donate?", Map.of("completedDonations", 5L));

        assertThat(reply).isEqualTo("Register, then post your donation.");
        verify(groqClient).complete(
                contains("FoodBot"),
                org.mockito.ArgumentMatchers.eq("How do I donate?"),
                anyDouble());
    }

    @Test
    @DisplayName("impact report passes the live metrics into the prompt")
    void generateImpactReport_referencesMetrics() {
        when(groqClient.complete(anyString(), anyString(), anyDouble()))
                .thenReturn("Great progress on waste reduction.");

        String report = aiService.generateImpactReport(Map.of("mealsServed", 120L));

        assertThat(report).isEqualTo("Great progress on waste reduction.");
        verify(groqClient).complete(
                contains("impact report"),
                contains("mealsServed=120"),
                anyDouble());
    }
}
