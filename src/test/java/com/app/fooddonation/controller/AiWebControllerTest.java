package com.app.fooddonation.controller;

import com.app.fooddonation.integration.GroqClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the web-facing AI endpoints (/ai/**). The real Groq
 * HTTP client is mocked so the suite never calls the paid API.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroqClient groqClient;

    @Test
    @WithMockUser(username = "donor@demo.com", roles = "DONOR")
    @DisplayName("generate an AI donation listing and return parsed JSON")
    void listing_generatesDescription() throws Exception {
        when(groqClient.complete(anyString(), anyString(), anyDouble()))
                .thenReturn("""
                        {"description":"Freshly cooked vegetarian biryani from tonight's buffet, hot and ready for pickup.","quantity":"20 boxes","bestBefore":"Within 2 hours"}
                        """);

        mockMvc.perform(post("/ai/listing")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"foodType":"Vegetarian","quantity":"20 boxes","city":"Mumbai","notes":"Buffet leftovers"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value(org.hamcrest.Matchers.containsString("biryani")))
                .andExpect(jsonPath("$.quantity").value("20 boxes"))
                .andExpect(jsonPath("$.bestBefore").value("Within 2 hours"));
    }

    @Test
    @WithMockUser(username = "ngo@demo.com", roles = "NGO")
    @DisplayName("chat with FoodBot returns the AI reply")
    void chat_returnsReply() throws Exception {
        when(groqClient.complete(anyString(), anyString(), anyDouble()))
                .thenReturn("To post a donation, register as a donor and click Create Donation.");

        mockMvc.perform(post("/ai/chat")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"How do I donate?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Create Donation")));
    }

    @Test
    @WithMockUser(username = "ngo@demo.com", roles = "NGO")
    @DisplayName("generate an AI impact report from live platform stats")
    void impactReport_returnsNarrative() throws Exception {
        when(groqClient.complete(anyString(), anyString(), anyDouble()))
                .thenReturn("Your community has served hundreds of meals thanks to every completed donation.");

        mockMvc.perform(post("/ai/impact-report")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("meals")));
    }

    @Test
    @DisplayName("anonymous users are redirected to the login page")
    void anonymousRequest_redirectsToLogin() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/ai/listing"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrlPattern("**/login"));
    }
}
