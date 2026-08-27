package com.clas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private static final long CONCURRENT_CLAIM_ORDER_ID = 990002L;
    private static final long DELIVERY_CYCLE_ORDER_ID = 990003L;
    private static final long RIDER_MERCHANT_CONTACT_ORDER_ID = 990004L;

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
    void availableTaskCanOnlyBeClaimedByOneRider() throws Exception {
        jdbcTemplate.update("DELETE FROM orders WHERE id = ?", CONCURRENT_CLAIM_ORDER_ID);
        jdbcTemplate.update("""
            INSERT INTO orders (
                id, user_id, merchant_id, total_price, subtotal, delivery_fee,
                coupon_discount, status, delivery_address, delivery_longitude, delivery_latitude,
                delivery_status, estimated_minutes, refund_status, create_time, accepted_at
            ) VALUES (?, '13800000001', 1, 2890, 2590, 300, 0, 'ACCEPTED',
                '软件学院 A 座 302', 116.398000, 39.910000, 'AVAILABLE', 30, 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, CONCURRENT_CLAIM_ORDER_ID);
        jdbcTemplate.update("""
            UPDATE rider_profile
            SET online_status = TRUE, accepting_orders = TRUE,
                current_longitude = 116.397428, current_latitude = 39.909230,
                location_updated_at = CURRENT_TIMESTAMP
            WHERE user_id IN ('13800000004', '13800000005')
            """);

        String firstRider = "Bearer " + switchRole(loginToken("13800000004"), "RIDER");
        String secondRider = "Bearer " + switchRole(loginToken("13800000005"), "RIDER");

        mockMvc.perform(get("/api/rider/tasks").header("Authorization", firstRider))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].orderId").value(org.hamcrest.Matchers.hasItem((int) CONCURRENT_CLAIM_ORDER_ID)));

        mockMvc.perform(post("/api/rider/tasks/{orderId}/claim", CONCURRENT_CLAIM_ORDER_ID).header("Authorization", firstRider))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.riderId").value("13800000004"))
            .andExpect(jsonPath("$.data.deliveryStatus").value("ASSIGNED_WAITING_MEAL"));

        mockMvc.perform(post("/api/rider/tasks/{orderId}/claim", CONCURRENT_CLAIM_ORDER_ID).header("Authorization", secondRider))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("配送任务已被领取或不可用"));

        org.junit.jupiter.api.Assertions.assertEquals("13800000004", jdbcTemplate.queryForObject(
            "SELECT rider_id FROM orders WHERE id = ?", String.class, CONCURRENT_CLAIM_ORDER_ID));
    }

    @Test
    void merchantRiderUserCanCompleteDeliveryCycleAndLeaveAuditableHistory() throws Exception {
        jdbcTemplate.update("DELETE FROM order_lifecycle_event WHERE order_id = ?", DELIVERY_CYCLE_ORDER_ID);
        jdbcTemplate.update("DELETE FROM rider_review WHERE order_id = ?", DELIVERY_CYCLE_ORDER_ID);
        jdbcTemplate.update("DELETE FROM review WHERE order_id = ?", DELIVERY_CYCLE_ORDER_ID);
        jdbcTemplate.update("DELETE FROM orders WHERE id = ?", DELIVERY_CYCLE_ORDER_ID);
        jdbcTemplate.update("""
            INSERT INTO orders (id, user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
                status, delivery_address, delivery_longitude, delivery_latitude, delivery_status, estimated_minutes,
                refund_status, create_time, paid_at)
            VALUES (?, '13800000001', 1, 2890, 2590, 300, 0, 'PAID', '软件学院 A 座 302',
                116.398000, 39.910000, 'WAITING', 30, 'NONE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, DELIVERY_CYCLE_ORDER_ID);
        jdbcTemplate.update("UPDATE merchant SET longitude = 116.397428, latitude = 39.909230 WHERE id = 1");
        jdbcTemplate.update("""
            UPDATE rider_profile SET online_status = TRUE, accepting_orders = TRUE, max_active_orders = 5,
                current_longitude = 116.397428, current_latitude = 39.909230, location_updated_at = CURRENT_TIMESTAMP
            WHERE user_id = '13800000004'
            """);

        String merchantAuth = "Bearer " + switchRole(loginToken("13800000002"), "MERCHANT");
        String riderAuth = "Bearer " + switchRole(loginToken("13800000004"), "RIDER");
        String userAuth = "Bearer " + switchRole(loginToken("13800000001"), "USER");
        String adminAuth = "Bearer " + loginToken("13800000003");

        mockMvc.perform(post("/api/order/accept/{orderId}", DELIVERY_CYCLE_ORDER_ID).header("Authorization", merchantAuth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.deliveryStatus").value("AVAILABLE"));
        mockMvc.perform(post("/api/rider/tasks/{orderId}/claim", DELIVERY_CYCLE_ORDER_ID).header("Authorization", riderAuth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.deliveryStatus").value("ASSIGNED_WAITING_MEAL"));
        mockMvc.perform(post("/api/rider/deliveries/{orderId}/pickup", DELIVERY_CYCLE_ORDER_ID).header("Authorization", riderAuth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.deliveryStatus").value("DELIVERING"));
        mockMvc.perform(post("/api/rider/deliveries/{orderId}/complete", DELIVERY_CYCLE_ORDER_ID).header("Authorization", riderAuth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.deliveryStatus").value("DELIVERED"));
        mockMvc.perform(post("/api/order/complete/{orderId}", DELIVERY_CYCLE_ORDER_ID).header("Authorization", userAuth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mockMvc.perform(post("/api/review/add").header("Authorization", userAuth).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("orderId", DELIVERY_CYCLE_ORDER_ID, "score", 5, "content", "餐品和配送都很好", "images", java.util.List.of()))))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/order/{orderId}/rider-review", DELIVERY_CYCLE_ORDER_ID).header("Authorization", userAuth).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("score", 5, "tags", "准时", "content", "送达及时"))))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/order/{orderId}/timeline", DELIVERY_CYCLE_ORDER_ID).header("Authorization", adminAuth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(7)))
            .andExpect(jsonPath("$.data[*].eventType").value(org.hamcrest.Matchers.hasItem("USER_CONFIRMED_RECEIPT")));
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

    @Test
    void riderCanUpdateNormalInformationAndServicePhoneNeedsAdminApproval() throws Exception {
        String riderAuth = "Bearer " + switchRole(loginToken("13800000004"), "RIDER");

        mockMvc.perform(get("/api/rider/info").header("Authorization", riderAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value("13800000004"));

        mockMvc.perform(put("/api/rider/info")
                .header("Authorization", riderAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "vehicleType", "MOTORCYCLE", "serviceArea", "大学城东区",
                    "emergencyContactName", "测试联系人", "emergencyContactPhone", "13900000063"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.vehicleType").value("MOTORCYCLE"))
            .andExpect(jsonPath("$.data.serviceArea").value("大学城东区"));

        MvcResult change = mockMvc.perform(post("/api/rider/info/service-phone-change")
                .header("Authorization", riderAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", "13900000064"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn();
        long changeId = objectMapper.readTree(change.getResponse().getContentAsString()).path("data").path("id").asLong();

        String adminAuth = "Bearer " + loginToken("13800000003");
        mockMvc.perform(get("/api/rider/admin/info-change-requests").header("Authorization", adminAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].id").value(org.hamcrest.Matchers.hasItem((int) changeId)));
        mockMvc.perform(patch("/api/rider/admin/info-change-requests/{id}", changeId)
                .header("Authorization", adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("approved", true, "reason", "资料核验通过"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/rider/info").header("Authorization", riderAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.servicePhone").value("13900000064"))
            .andExpect(jsonPath("$.data.latestPhoneChange.status").value("APPROVED"));
    }

    @Test
    void assignedRiderAndMerchantCanExchangeOrderMessages() throws Exception {
        jdbcTemplate.update("DELETE FROM chat_message WHERE order_id = ?", RIDER_MERCHANT_CONTACT_ORDER_ID);
        jdbcTemplate.update("DELETE FROM chat_conversation WHERE order_id = ?", RIDER_MERCHANT_CONTACT_ORDER_ID);
        jdbcTemplate.update("DELETE FROM orders WHERE id = ?", RIDER_MERCHANT_CONTACT_ORDER_ID);
        jdbcTemplate.update("""
            INSERT INTO orders (
                id, user_id, merchant_id, rider_id, total_price, subtotal, delivery_fee,
                coupon_discount, status, delivery_address, delivery_status,
                estimated_minutes, refund_status, create_time, accepted_at, rider_assigned_at
            ) VALUES (?, '13800000001', 1, '13800000004', 2890, 2590, 300, 0, 'ACCEPTED',
                '软件学院 A 座 302', 'ASSIGNED_WAITING_MEAL', 30, 'NONE', CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, RIDER_MERCHANT_CONTACT_ORDER_ID);

        String riderAuth = "Bearer " + switchRole(loginToken("13800000004"), "RIDER");
        String merchantAuth = "Bearer " + loginToken("13800000002");

        mockMvc.perform(post("/api/delivery/orders/{orderId}/merchant-messages", RIDER_MERCHANT_CONTACT_ORDER_ID)
                .header("Authorization", riderAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "我已到店，麻烦确认出餐进度。"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.senderRole").value("RIDER"));

        mockMvc.perform(post("/api/delivery/orders/{orderId}/merchant-messages", RIDER_MERCHANT_CONTACT_ORDER_ID)
                .header("Authorization", merchantAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "预计两分钟出餐。"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.senderRole").value("MERCHANT"));

        mockMvc.perform(get("/api/delivery/orders/{orderId}/merchant-messages", RIDER_MERCHANT_CONTACT_ORDER_ID)
                .header("Authorization", riderAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[1].content").value("预计两分钟出餐。"));
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
            .andReturn();
        if (result.getResponse().getStatus() == 409) {
            mockMvc.perform(post("/api/user/login/send-code")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("phone", phone))))
                .andExpect(status().isOk());
            result = mockMvc.perform(post("/api/user/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "phone", phone,
                        "password", "Abc123!",
                        "code", "123456"
                    ))))
                .andExpect(status().isOk())
                .andReturn();
        } else {
            org.junit.jupiter.api.Assertions.assertEquals(200, result.getResponse().getStatus());
        }
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("token").asText();
    }
}
