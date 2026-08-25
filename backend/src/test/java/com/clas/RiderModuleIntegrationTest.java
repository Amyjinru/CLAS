package com.clas;

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
class RiderModuleIntegrationTest {
    private static final long ORDER_ID = 990001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void riderCanListAndClaimPreparingOrder() throws Exception {
        jdbcTemplate.update("DELETE FROM orders WHERE id = ?", ORDER_ID);
        jdbcTemplate.update("""
            INSERT INTO orders (
                id, user_id, merchant_id, total_price, subtotal, delivery_fee,
                coupon_discount, status, delivery_address, delivery_status,
                estimated_minutes, refund_status, create_time, accepted_at
            ) VALUES (?, '13800000001', 1, 2890, 2590, 300, 0, 'ACCEPTED',
                '软件学院 A 座 302', 'PREPARING', 30, 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, ORDER_ID);

        String riderAuth = "Bearer " + switchRole(loginToken("13800000004"), "RIDER");

        mockMvc.perform(get("/api/user/profile").header("Authorization", riderAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.phone").value("13800000004"));
        mockMvc.perform(get("/api/notifications/mine").header("Authorization", riderAuth))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/rider/orders/available").header("Authorization", riderAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].order.id").value(org.hamcrest.Matchers.hasItem((int) ORDER_ID)));

        mockMvc.perform(post("/api/rider/orders/{orderId}/claim", ORDER_ID).header("Authorization", riderAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.riderId").value("13800000004"))
            .andExpect(jsonPath("$.data.order.deliveryStatus").value("ASSIGNED"));

        mockMvc.perform(post("/api/rider/orders/{orderId}/claim", ORDER_ID).header("Authorization", riderAuth))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("订单已被其他骑手接走或状态已变化"));

        String userAuth = "Bearer " + switchRole(loginToken("13800000004"), "USER");
        mockMvc.perform(get("/api/user/profile").header("Authorization", userAuth))
            .andExpect(status().isOk());
    }

    @Test
    void userBecomesRiderOnlyAfterApplicationIsApproved() throws Exception {
        String phone = "13900000061";
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", phone))))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", phone, "username", "rider_applicant", "password", "Abc123!",
                    "confirmPassword", "Abc123!", "code", "123456"
                ))))
            .andExpect(status().isOk());

        String userAuth = "Bearer " + loginToken(phone);
        MvcResult application = mockMvc.perform(post("/api/role-applications/rider")
                .header("Authorization", userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("reason", "有配送经验，可在校区服务"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn();
        long applicationId = objectMapper.readTree(application.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        mockMvc.perform(get("/api/rider/orders/me").header("Authorization", userAuth))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/role-applications/admin/{id}/audit", applicationId)
                .header("Authorization", "Bearer " + loginToken("13800000003"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED", "remarks", "资料符合要求"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"));

        String riderAuth = "Bearer " + switchRole(loginToken(phone), "RIDER");
        mockMvc.perform(get("/api/rider/orders/me").header("Authorization", riderAuth))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/role-applications/mine").header("Authorization", riderAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value("rider-" + applicationId))
            .andExpect(jsonPath("$.data[0].targetRole").value("RIDER"))
            .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    private String switchRole(String token, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/switch-role")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", role))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("token").asText();
    }

    private String loginToken(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", phone,
                    "password", "Abc123!"
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("token").asText();
    }
}
