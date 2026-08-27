package com.clas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
