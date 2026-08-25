package com.clas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class CancellationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void cancellingBusinessRoleOnlyRemovesThatPortalAccess() throws Exception {
        String phone = "13900000062";
        register(phone);
        jdbcTemplate.update("INSERT INTO user_role (user_id, role) VALUES (?, 'RIDER')", phone);
        jdbcTemplate.update("""
            INSERT INTO role_application (user_id, target_role, reason, status, created_at, updated_at)
            VALUES (?, 'RIDER', '此前已通过的申请', 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, phone);

        mockMvc.perform(post("/api/user/roles/cancel")
                .header("Authorization", "Bearer " + loginToken(phone))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "RIDER"))))
            .andExpect(status().isOk());

        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_role WHERE user_id = ? AND role = 'RIDER'", Integer.class, phone));
        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM \"user\" WHERE phone = ?", Integer.class, phone));

        mockMvc.perform(post("/api/role-applications/rider")
                .header("Authorization", "Bearer " + loginToken(phone))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "重新申请骑手身份"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void cancellingAccountRemovesUserAndPersonalRecords() throws Exception {
        String phone = "13900000063";
        register(phone);
        jdbcTemplate.update("INSERT INTO cart (user_id, product_id, quantity) VALUES (?, 1, 2)", phone);

        mockMvc.perform(post("/api/user/account/cancel")
                .header("Authorization", "Bearer " + loginToken(phone))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "currentPassword", "Abc123!", "confirmation", "注销账户"
                ))))
            .andExpect(status().isOk());

        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM \"user\" WHERE phone = ?", Integer.class, phone));
        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM cart WHERE user_id = ?", Integer.class, phone));
        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_role WHERE user_id = ?", Integer.class, phone));
        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", phone, "password", "Abc123!"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("手机号或密码错误"));
    }

    @Test
    void pendingMerchantAndRiderApplicationsAreMutuallyExclusive() throws Exception {
        String merchantApplicant = "13900000064";
        register(merchantApplicant);
        jdbcTemplate.update("""
            INSERT INTO merchant (user_id, merchant_name, phone, delivery_radius_m, delivery_fee,
                min_order_price, average_price, status, created_at, updated_at)
            VALUES (?, 'Pending Merchant', '13800000064', 3000, 0, 0, 0, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, merchantApplicant);
        mockMvc.perform(get("/api/role-applications/mine")
                .header("Authorization", "Bearer " + loginToken(merchantApplicant)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(org.hamcrest.Matchers.startsWith("merchant-")))
            .andExpect(jsonPath("$.data[0].targetRole").value("MERCHANT"))
            .andExpect(jsonPath("$.data[0].status").value("PENDING"));
        mockMvc.perform(post("/api/role-applications/rider")
                .header("Authorization", "Bearer " + loginToken(merchantApplicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "申请骑手"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("已有待审核的商家申请，暂不能申请骑手身份"));

        String riderApplicant = "13900000065";
        register(riderApplicant);
        jdbcTemplate.update("""
            INSERT INTO role_application (user_id, target_role, reason, status, created_at, updated_at)
            VALUES (?, 'RIDER', '申请骑手', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, riderApplicant);
        mockMvc.perform(post("/api/merchant/register")
                .header("Authorization", "Bearer " + loginToken(riderApplicant))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "merchantName", "Blocked Merchant", "contactPhone", "13800000065",
                    "category", "餐饮", "address", "校区东门", "longitude", 116.397428,
                    "latitude", 39.909230, "deliveryRadiusM", 3000
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("已有待审核的骑手申请，暂不能申请商家身份"));
    }

    @Test
    void cancelledMerchantCanSubmitANewApplication() throws Exception {
        String phone = "13900000066";
        register(phone);
        jdbcTemplate.update("""
            INSERT INTO merchant (user_id, merchant_name, phone, delivery_radius_m, delivery_fee,
                min_order_price, average_price, status, created_at, updated_at)
            VALUES (?, 'Cancelled Merchant', '13800000066', 3000, 0, 0, 0, 'DISABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, phone);

        mockMvc.perform(post("/api/merchant/register")
                .header("Authorization", "Bearer " + loginToken(phone))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "merchantName", "Reapplied Merchant", "contactPhone", "13800000067",
                    "category", "餐饮", "address", "校区西门", "longitude", 116.397428,
                    "latitude", 39.909230, "deliveryRadiusM", 3000
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM merchant WHERE user_id = ?", Integer.class, phone));
        assertEquals("PENDING", jdbcTemplate.queryForObject(
            "SELECT status FROM merchant WHERE user_id = ?", String.class, phone));
    }

    private void register(String phone) throws Exception {
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", phone))))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", phone, "username", "cancel_test", "password", "Abc123!",
                    "confirmPassword", "Abc123!", "code", "123456"
                ))))
            .andExpect(status().isOk());
    }

    private String loginToken(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", phone, "password", "Abc123!"))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("token").asText();
    }
}
