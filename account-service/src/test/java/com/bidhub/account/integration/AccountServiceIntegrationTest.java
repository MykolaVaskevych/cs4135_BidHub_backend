package com.bidhub.account.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.User;
import com.bidhub.account.model.UserRole;
import com.bidhub.account.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AccountServiceIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDb() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Full flow: register → login → get profile → update → change password → re-login")
    void fullUserLifecycle() throws Exception {
        // 1. Register
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"integ@test.com","password":"Integ123!",
                             "firstName":"Int","lastName":"Test","role":"BUYER"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("BUYER"))
                .andReturn();

        String userId = extractField(regResult, "userId");

        // 2. Login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"integ@test.com","password":"Integ123!"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        // 3. Get profile (via header — simulating gateway injection)
        mockMvc.perform(get("/api/accounts/me")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("integ@test.com"))
                .andExpect(jsonPath("$.firstName").value("Int"));

        // 4. Update profile
        mockMvc.perform(put("/api/accounts/me")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"firstName":"Updated","lastName":"Name"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));

        // 5. Change password
        mockMvc.perform(put("/api/accounts/me/password")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"currentPassword":"Integ123!","newPassword":"NewInteg1!"}
                            """))
                .andExpect(status().isNoContent());

        // 6. Login with new password
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"integ@test.com","password":"NewInteg1!"}
                            """))
                .andExpect(status().isOk());

        // 7. Old password fails
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"integ@test.com","password":"Integ123!"}
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin flow: seed admin → list users → suspend → reactivate → ban")
    void adminUserManagement() throws Exception {
        // Seed admin
        User admin = new User();
        admin.setEmail("admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        userRepository.save(admin);

        // Seed target
        User target = new User();
        target.setEmail("target@test.com");
        target.setPasswordHash(passwordEncoder.encode("Target1!"));
        target.setFirstName("Target");
        target.setLastName("User");
        target.setRole(UserRole.BUYER);
        target.setStatus(AccountStatus.ACTIVE);
        target = userRepository.save(target);

        String adminId = admin.getUserId().toString();
        String targetId = target.getUserId().toString();

        // List users
        mockMvc.perform(get("/api/admin/users")
                        .header("X-User-Id", adminId)
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Suspend target
        mockMvc.perform(post("/api/admin/users/" + targetId + "/suspend")
                        .header("X-User-Id", adminId)
                        .header("X-User-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"reason":"test suspend"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // Verify suspended user cannot login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"target@test.com","password":"Target1!"}
                            """))
                .andExpect(status().isForbidden());

        // Reactivate
        mockMvc.perform(post("/api/admin/users/" + targetId + "/reactivate")
                        .header("X-User-Id", adminId)
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Ban
        mockMvc.perform(post("/api/admin/users/" + targetId + "/ban")
                        .header("X-User-Id", adminId)
                        .header("X-User-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"reason":"permanent ban"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BANNED"));

        // Self-action rejected
        mockMvc.perform(post("/api/admin/users/" + adminId + "/suspend")
                        .header("X-User-Id", adminId)
                        .header("X-User-Roles", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"reason":"self"}
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Address flow: create → list → set default → delete with auto-promote")
    void addressCrud() throws Exception {
        // Register user
        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"addr@test.com","password":"Addr1234!",
                             "firstName":"A","lastName":"B","role":"BUYER"}
                            """))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = extractField(reg, "userId");

        // Create first address (auto-default)
        MvcResult a1 = mockMvc.perform(post("/api/accounts/me/addresses")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"addressLine1":"1 Main","city":"Dublin","county":"Dublin","eircode":"D01"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true))
                .andReturn();
        String addr1Id = extractField(a1, "addressId");

        // Create second address
        MvcResult a2 = mockMvc.perform(post("/api/accounts/me/addresses")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"addressLine1":"2 Oak","city":"Cork","county":"Cork","eircode":"T12"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(false))
                .andReturn();
        String addr2Id = extractField(a2, "addressId");

        // List — should have 2
        mockMvc.perform(get("/api/accounts/me/addresses")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Set addr2 as default
        mockMvc.perform(put("/api/accounts/me/addresses/" + addr2Id + "/default")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));

        // Delete addr2 (default) — addr1 should auto-promote
        mockMvc.perform(delete("/api/accounts/me/addresses/" + addr2Id)
                        .header("X-User-Id", userId))
                .andExpect(status().isNoContent());

        // Verify addr1 is now default
        mockMvc.perform(get("/api/accounts/me")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addresses[0].isDefault").value(true));
    }

    @Test
    @DisplayName("Cross-service: GET /api/accounts/{userId} returns user for any authenticated caller")
    void crossServiceLookup() throws Exception {
        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"lookup@test.com","password":"Lookup1!x",
                             "firstName":"Look","lastName":"Up","role":"SELLER"}
                            """))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = extractField(reg, "userId");

        // Any authenticated user can look up by ID
        mockMvc.perform(get("/api/accounts/" + userId)
                        .header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("lookup@test.com"));
    }

    @Test
    @DisplayName("Validation: weak password rejected, duplicate email rejected")
    void validationErrors() throws Exception {
        // Weak password
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"v@test.com","password":"nodigit!",
                             "firstName":"V","lastName":"V","role":"BUYER"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

        // Register once
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"dup@test.com","password":"Dup12345!",
                             "firstName":"D","lastName":"U","role":"BUYER"}
                            """))
                .andExpect(status().isCreated());

        // Duplicate
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"dup@test.com","password":"Dup12345!",
                             "firstName":"D","lastName":"U","role":"BUYER"}
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"));
    }

    private String extractField(MvcResult result, String field) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get(field).asText();
    }

    private static class UUID {
        static String randomUUID() {
            return java.util.UUID.randomUUID().toString();
        }
    }
}
