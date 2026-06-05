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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ModuleIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userRegisterWorksAndHidesPassword() throws Exception {
        // 注册成功时默认角色应为 USER，响应中不能把明文密码带回前端。
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "new_user",
                    "password", "123456",
                    "phone", "13900000010"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("new_user"))
            .andExpect(jsonPath("$.data.role").value("USER"))
            .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void userRegisterRejectsDuplicateUsername() throws Exception {
        // 用户名是登录主键，重复注册必须被业务层拦截。
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "user",
                    "password", "123456",
                    "phone", "13900000011"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void userRegisterRejectsDuplicatePhone() throws Exception {
        // 手机号也要保持唯一，避免不同账号共享同一份身份资料。
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "phone_conflict_user",
                    "password", "123456",
                    "phone", "13800000001"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("手机号已存在"));
    }

    @Test
    void userRegisterRejectsUnknownRole() throws Exception {
        // 角色字段只允许项目约定的三种值，防止其他同学误造状态名。
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "bad_role_user",
                    "password", "123456",
                    "role", "ROOT"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("角色只能是 USER、MERCHANT 或 ADMIN"));
    }

    @Test
    void userLoginWorksAndRejectsBadPassword() throws Exception {
        // 登录成功返回当前用户资料；登录失败统一返回业务错误。
        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "user",
                    "password", "123456"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.user.username").value("user"))
            .andExpect(jsonPath("$.data.user.password").doesNotExist());

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "user",
                    "password", "wrong-password"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void adminMerchantListRequiresAdminRole() throws Exception {
        // 管理员接口必须同时拦截未登录用户和非 ADMIN 角色用户。
        mockMvc.perform(get("/api/merchant/admin/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("未登录，请先登录"));

        mockMvc.perform(get("/api/merchant/admin/list")
                .header("Authorization", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("权限不足，无法访问"));

        mockMvc.perform(get("/api/merchant/admin/list")
                .header("Authorization", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void announcementListWorks() throws Exception {
        mockMvc.perform(get("/api/announcement/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void paymentReviewFlowWorks() throws Exception {
        mockMvc.perform(post("/api/cart/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", 1,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        MvcResult orderResult = mockMvc.perform(post("/api/order/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", 1,
                    "merchantId", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.status").value("PENDING_PAYMENT"))
            .andReturn();

        mockMvc.perform(get("/api/product/list/1"))
            .andExpect(jsonPath("$.data[0].stock").value(29));

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();

        mockMvc.perform(get("/api/payment/status/" + orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.orderStatus").value("PENDING_PAYMENT"));

        mockMvc.perform(post("/api/payment/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", 1,
                    "payMethod", "MOCK"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        mockMvc.perform(post("/api/order/accept/" + orderId))
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/order/complete/" + orderId))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/review/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", 1,
                    "score", 5,
                    "content", "集成测试评价"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.score").value(5));

        mockMvc.perform(get("/api/review/rating/1"))
            .andExpect(jsonPath("$.data.reviewCount").value(1))
            .andExpect(jsonPath("$.data.averageScore").value(5.0));

        mockMvc.perform(post("/api/review/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", 1,
                    "score", 3,
                    "content", "重复评价"
                ))))
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createAnnouncementWorks() throws Exception {
        mockMvc.perform(post("/api/announcement/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "新公告",
                    "content", "公告内容"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("新公告"));
    }
}
