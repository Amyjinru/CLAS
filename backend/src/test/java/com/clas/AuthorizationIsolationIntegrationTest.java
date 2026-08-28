package com.clas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "clas.demo-accounts.access-password=test-only-demo-access")
class AuthorizationIsolationIntegrationTest {
    private static final String ADMIN_PHONE = "13800000003";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void databaseRoleOverridesRoleEmbeddedInExistingToken() throws Exception {
        String oldAdminToken = loginToken(ADMIN_PHONE);
        jdbcTemplate.update("UPDATE \"user\" SET role = 'USER' WHERE phone = ?", ADMIN_PHONE);
        try {
            mockMvc.perform(get("/api/admin/users")
                    .header("Authorization", "Bearer " + oldAdminToken))
                .andExpect(status().isForbidden());
        } finally {
            jdbcTemplate.update("UPDATE \"user\" SET role = 'ADMIN' WHERE phone = ?", ADMIN_PHONE);
        }
    }

    @Test
    void latestLoginInvalidatesPreviousDeviceToken() throws Exception {
        String previousDeviceToken = loginToken(ADMIN_PHONE);
        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", ADMIN_PHONE,
                    "password", "Abc123!"
                ))))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/user/login/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", ADMIN_PHONE))))
            .andExpect(status().isOk());
        String currentDeviceToken = loginTokenWithCode(ADMIN_PHONE, "123456");

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + previousDeviceToken))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + currentDeviceToken))
            .andExpect(status().isOk());
    }

    @Test
    void sameDeviceReloginSkipsVerificationWhileAnotherDeviceCreatesNotice() throws Exception {
        jdbcTemplate.update("UPDATE \"user\" SET session_token = NULL, session_expires_at = NULL, session_device_id = NULL, "
            + "session_last_seen_at = NULL, pending_login_challenge_id = NULL, pending_login_device_id = NULL, "
            + "pending_login_created_at = NULL WHERE phone = ?", ADMIN_PHONE);

        String currentToken = loginTokenOnDevice(ADMIN_PHONE, "browser-a");
        String sameDeviceToken = loginTokenOnDevice(ADMIN_PHONE, "browser-a");

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", ADMIN_PHONE,
                    "password", "Abc123!",
                    "deviceId", "browser-b"
                ))))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/user/login-notice")
                .header("Authorization", "Bearer " + sameDeviceToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.challengeId").isNotEmpty());

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + currentToken))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + sameDeviceToken))
            .andExpect(status().isOk());
    }

    @Test
    void demoLoginUsesTheSameSingleDeviceVerificationRule() throws Exception {
        String riderPhone = "13800000005";
        jdbcTemplate.update("UPDATE \"user\" SET session_token = NULL, session_expires_at = NULL, session_device_id = NULL, "
            + "session_last_seen_at = NULL, pending_login_challenge_id = NULL, pending_login_device_id = NULL, "
            + "pending_login_created_at = NULL WHERE phone = ?", riderPhone);
        mockMvc.perform(post("/api/user/demo-access/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "wrong-password"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/user/demo-access/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "test-only-demo-access"))))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", riderPhone, "deviceId", "demo-a", "accessPassword", "test-only-demo-access"))))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", riderPhone, "deviceId", "demo-b", "accessPassword", "test-only-demo-access"))))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/api/user/login/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", riderPhone))))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/demo-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", riderPhone, "code", "123456", "deviceId", "demo-b", "accessPassword", "test-only-demo-access"))))
            .andExpect(status().isOk());
    }

    private String loginToken(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", phone,
                    "password", "Abc123!"
                ))))
            .andReturn();
        if (result.getResponse().getStatus() == 409) {
            mockMvc.perform(post("/api/user/login/send-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("phone", phone))))
                .andExpect(status().isOk());
            return loginTokenWithCode(phone, "123456");
        }
        org.junit.jupiter.api.Assertions.assertEquals(200, result.getResponse().getStatus());
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("token").asText();
    }

    private String loginTokenOnDevice(String phone, String deviceId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", phone,
                    "password", "Abc123!",
                    "deviceId", deviceId
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("token").asText();
    }

    private String loginTokenWithCode(String phone, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", phone,
                    "password", "Abc123!",
                    "code", code
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("token").asText();
    }
}
