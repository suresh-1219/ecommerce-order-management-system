package com.suresh.ecommerce.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suresh.ecommerce.dto.LoginDTO;
import com.suresh.ecommerce.dto.TokenRefreshRequestDTO;
import com.suresh.ecommerce.dto.UserRegisterDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration test — runs the real Spring context, real Spring Security
 * filter chain, and a real (in-memory H2) database, exercising the auth flow through
 * actual HTTP requests via MockMvc rather than mocking any layer. This complements the
 * unit tests: unit tests verify a service's logic in isolation, this verifies the pieces
 * are wired together correctly end-to-end (controller -> security -> service -> repository -> DB).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Created directly rather than @Autowired — Spring's own ObjectMapper bean wasn't resolving
    // in this test context under Spring Boot 4's modularized Jackson autoconfiguration, and a
    // plain instance is all this test needs (simple POJO DTOs, no custom Jackson modules required).
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserRegisterDTO newRegisterDto(String email) {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setName("Integration Test User");
        dto.setEmail(email);
        dto.setPassword("testPassword123");
        return dto;
    }

    @Test
    void register_thenLogin_thenRefresh_fullFlowSucceeds() throws Exception {
        UserRegisterDTO registerDto = newRegisterDto("integration.flow@example.com");

        // 1. Register
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("integration.flow@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        // 2. Login with the same credentials
        LoginDTO loginDto = new LoginDTO();
        loginDto.setEmail("integration.flow@example.com");
        loginDto.setPassword("testPassword123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        // 3. Exchange the refresh token for a new JWT
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        UserRegisterDTO dto = newRegisterDto("duplicate@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // Same email again
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidPayload_returns400WithFieldErrors() throws Exception {
        UserRegisterDTO invalid = new UserRegisterDTO();
        invalid.setName("");
        invalid.setEmail("not-an-email");
        invalid.setPassword("123"); // too short

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        UserRegisterDTO registerDto = newRegisterDto("wrongpass@example.com");
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated());

        LoginDTO loginDto = new LoginDTO();
        loginDto.setEmail("wrongpass@example.com");
        loginDto.setPassword("theWrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_unknownToken_returns401() throws Exception {
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken("this-token-does-not-exist");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_thenRefreshWithSameToken_returns401() throws Exception {
        UserRegisterDTO registerDto = newRegisterDto("logout.flow@example.com");
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated());

        LoginDTO loginDto = new LoginDTO();
        loginDto.setEmail("logout.flow@example.com");
        loginDto.setPassword("testPassword123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginResponse).get("token").asText();
        String refreshToken = objectMapper.readTree(loginResponse).get("refreshToken").asText();

        // Logout requires a valid access token (it's a protected endpoint)
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // The refresh token was revoked server-side — it must no longer work, even
        // though the access token itself hasn't expired yet.
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }
}
