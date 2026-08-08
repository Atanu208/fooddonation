package com.app.fooddonation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the stateless REST API: registration -> JWT login ->
 * create donation -> NGO acceptance (with concurrency guard) -> status flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String email, String role) throws Exception {
        java.util.Map<String, Object> registration = new java.util.HashMap<>();
        registration.put("name", "Test " + role);
        registration.put("email", email);
        registration.put("password", "secret123");
        registration.put("confirmPassword", "secret123");
        registration.put("role", role);
        registration.put("city", "Test City");
        registration.put("state", "TS");
        registration.put("pincode", "500001");
        registration.put("organizationName", role.equals("NGO") ? "Test NGO" : null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated());

        return login(email);
    }

    private String login(String email) throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("email", email, "password", "secret123"));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    @DisplayName("full API flow: register, login, create, accept, transition")
    void fullDonationFlow() throws Exception {
        String donorToken = registerAndLogin("flow-donor@test.com", "DONOR");
        String ngoToken = registerAndLogin("flow-ngo@test.com", "NGO");

        // /me with token
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow-donor@test.com"));

        // Create donation
        String donationBody = objectMapper.writeValueAsString(java.util.Map.of(
                "foodDescription", "20 boxes of vegetarian biryani",
                "pickupAddress", "Test Address",
                "pickupCity", "Test City",
                "pickupState", "TS",
                "pickupPincode", "500001",
                "pickupTime", "2026-08-20T10:00:00",
                "quantity", "20 boxes",
                "foodType", "Vegetarian",
                "isPackaged", true));

        MvcResult createResult = mockMvc.perform(post("/api/v1/donations")
                        .header("Authorization", "Bearer " + donorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(donationBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        long donationId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Accept with NGO
        mockMvc.perform(post("/api/v1/donations/{id}/accept", donationId)
                        .header("Authorization", "Bearer " + ngoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Second NGO trying to claim the same donation -> 409 conflict
        String ngo2Token = registerAndLogin("flow-ngo2@test.com", "NGO");
        mockMvc.perform(post("/api/v1/donations/{id}/accept", donationId)
                        .header("Authorization", "Bearer " + ngo2Token))
                .andExpect(status().isConflict());

        // Legal transition ACCEPTED -> PICKED_UP
        mockMvc.perform(put("/api/v1/donations/{id}/status", donationId)
                        .param("status", "PICKED_UP")
                        .header("Authorization", "Bearer " + ngoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));

        // Illegal transition ACCEPTED state is gone; PICKED_UP -> COMPLETED is invalid
        mockMvc.perform(put("/api/v1/donations/{id}/status", donationId)
                        .param("status", "COMPLETED")
                        .header("Authorization", "Bearer " + ngoToken))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("rejects requests without a token with a JSON 401")
    void unauthenticatedRequest_returns401Json() throws Exception {
        mockMvc.perform(get("/api/v1/donations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/json")));
    }

    @Test
    @DisplayName("rejects a wrong password with 401")
    void loginWithWrongPassword_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("email", "nobody@test.com", "password", "wrong"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejects an invalid registration payload with structured 400")
    void invalidRegistration_returns400WithFieldErrors() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "X",
                "email", "not-an-email",
                "password", "123",
                "confirmPassword", "456",
                "role", "DONOR"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    @DisplayName("admin endpoints reject non-admin users")
    void adminEndpoint_deniedForDonor() throws Exception {
        String donorToken = registerAndLogin("admin-test-donor@test.com", "DONOR");
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isForbidden());
    }
}
