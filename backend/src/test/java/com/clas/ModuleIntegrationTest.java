package com.clas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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
    private static final String USER_PHONE = "13800000001";
    private static final String MERCHANT_PHONE = "13800000002";
    private static final String ADMIN_PHONE = "13800000003";
    private static final String TEST_CODE = "123456";
    private static final String STRONG_PASSWORD = "Abc123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userRegisterWorksAndHidesPassword() throws Exception {
        // 注册成功时默认角色应为 USER，响应中不能把明文密码带回前端。
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", "13900000010"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "new_user",
                    "password", STRONG_PASSWORD,
                    "confirmPassword", STRONG_PASSWORD,
                    "phone", "13900000010",
                    "code", TEST_CODE
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("new_user"))
            .andExpect(jsonPath("$.data.role").value("USER"))
            .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void userRegisterAllowsDuplicateUsername() throws Exception {
        // 用户名只是展示名，允许重复；手机号才是账号主键。
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", "13900000011"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "user",
                    "password", STRONG_PASSWORD,
                    "confirmPassword", STRONG_PASSWORD,
                    "phone", "13900000011",
                    "code", TEST_CODE
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.phone").value("13900000011"))
            .andExpect(jsonPath("$.data.username").value("user"));
    }

    @Test
    void userRegisterRejectsDuplicatePhone() throws Exception {
        // 手机号也要保持唯一，避免不同账号共享同一份身份资料。
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", USER_PHONE))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("该手机号已被注册"));
    }

    @Test
    void userRegisterRejectsUnknownRole() throws Exception {
        // 角色字段只允许项目约定的三种值，防止其他同学误造状态名。
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", "13900000012"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "bad_role_user",
                    "password", STRONG_PASSWORD,
                    "confirmPassword", STRONG_PASSWORD,
                    "phone", "13900000012",
                    "code", TEST_CODE,
                    "role", "ROOT"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("角色只能是 USER、MERCHANT 或 ADMIN"));
    }

    @Test
    void userRegisterRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", "13900000013"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "weak_password_user",
                    "password", "abc123!",
                    "confirmPassword", "abc123!",
                    "phone", "13900000013",
                    "code", TEST_CODE
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("密码至少6位，必须包含大小写英文字母、数字和特殊符号，且不能包含空白字符"));
    }

    @Test
    void userRegisterRejectsMismatchedConfirmPassword() throws Exception {
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", "13900000016"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "mismatch_user",
                    "password", STRONG_PASSWORD,
                    "confirmPassword", "Abc123!!",
                    "phone", "13900000016",
                    "code", TEST_CODE
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("两次输入的密码不一致"));
    }

    @Test
    void merchantRegisterCreatesMerchantAccountWithSeparateContactPhone() throws Exception {
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", "13900000014"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/merchant/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.ofEntries(
                    Map.entry("accountPhone", "13900000014"),
                    Map.entry("code", TEST_CODE),
                    Map.entry("username", "merchant_new"),
                    Map.entry("password", STRONG_PASSWORD),
                    Map.entry("confirmPassword", STRONG_PASSWORD),
                    Map.entry("merchantName", "测试新商家"),
                    Map.entry("contactPhone", "13900000015"),
                    Map.entry("category", "美食"),
                    Map.entry("address", "测试地址 1 号"),
                    Map.entry("longitude", 116.390000),
                    Map.entry("latitude", 39.910000),
                    Map.entry("deliveryRadiusM", 3000),
                    Map.entry("bankAccount", "123456789"),
                    Map.entry("settlementCycle", 7)
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.userId").value("13900000014"))
            .andExpect(jsonPath("$.data.phone").value("13900000015"))
            .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void userLoginWorksAndRejectsBadPassword() throws Exception {
        // 登录成功返回当前用户资料；登录失败统一返回业务错误。
        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", USER_PHONE,
                    "password", STRONG_PASSWORD
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.user.username").value("user"))
            .andExpect(jsonPath("$.data.user.password").doesNotExist());

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", USER_PHONE,
                    "password", "wrong-password"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("手机号或密码错误"));
    }

    @Test
    void adminMerchantListRequiresAdminRole() throws Exception {
        // 管理员接口必须同时拦截未登录用户和非 ADMIN 角色用户。
        mockMvc.perform(get("/api/merchant/admin/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("未登录，请先登录"));

        mockMvc.perform(get("/api/merchant/admin/list")
                .header("Authorization", USER_PHONE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("权限不足，无法访问"));

        mockMvc.perform(get("/api/merchant/admin/list")
                .header("Authorization", ADMIN_PHONE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void merchantListCalculatesDistanceFromAddress() throws Exception {
        mockMvc.perform(get("/api/merchant/list")
                .header("Authorization", USER_PHONE)
                .param("addressId", "1")
                .param("sort", "distance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].distanceMeters").isNumber())
            .andExpect(jsonPath("$.data[0].estimatedMinutes").isNumber())
            .andExpect(jsonPath("$.data[0].deliveryAvailable").value(true));
    }

    @Test
    void merchantDeliveryEstimateWorksWithoutAmapKey() throws Exception {
        mockMvc.perform(get("/api/merchant/1/delivery-estimate")
                .param("lat", "39.910000")
                .param("lng", "116.398000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.merchantId").value(1))
            .andExpect(jsonPath("$.data.distanceMeters").isNumber())
            .andExpect(jsonPath("$.data.estimatedMinutes").isNumber())
            .andExpect(jsonPath("$.data.deliveryAvailable").value(true));
    }

    @Test
    void announcementListWorks() throws Exception {
        mockMvc.perform(get("/api/announcement/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void cartUpdateAndDeleteItemWork() throws Exception {
        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/cart/update")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 2
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].quantity").value(2))
            .andExpect(jsonPath("$.data[0].subtotal").value(5180));

        mockMvc.perform(delete("/api/cart/item/" + USER_PHONE + "/1")
                .header("Authorization", USER_PHONE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void cancelPendingOrderRestoresStock() throws Exception {
        MvcResult beforeStockResult = mockMvc.perform(get("/api/product/list/1"))
            .andExpect(status().isOk())
            .andReturn();
        int beforeStock = objectMapper.readTree(beforeStockResult.getResponse().getContentAsString())
            .path("data").get(0).path("stock").asInt();

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk());

        MvcResult orderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "merchantId", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.status").value("PENDING_PAYMENT"))
            .andReturn();

        mockMvc.perform(get("/api/product/list/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].stock").value(beforeStock - 1));

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();

        mockMvc.perform(post("/api/order/cancel/" + orderId)
                .header("Authorization", USER_PHONE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELED"));

        MvcResult afterStockResult = mockMvc.perform(get("/api/product/list/1"))
            .andExpect(status().isOk())
            .andReturn();
        int afterStock = objectMapper.readTree(afterStockResult.getResponse().getContentAsString())
            .path("data").get(0).path("stock").asInt();
        assertEquals(beforeStock, afterStock);
    }

    @Test
    void paymentReviewFlowWorks() throws Exception {
        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        MvcResult orderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "merchantId", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.status").value("PENDING_PAYMENT"))
            .andReturn();

        mockMvc.perform(get("/api/product/list/1"))
            .andExpect(jsonPath("$.data[0].stock").value(29));

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();

        mockMvc.perform(get("/api/payment/status/" + orderId)
                .header("Authorization", USER_PHONE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.orderStatus").value("PENDING_PAYMENT"));

        mockMvc.perform(post("/api/payment/mock")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", USER_PHONE,
                    "payMethod", "MOCK"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        mockMvc.perform(post("/api/order/accept/" + orderId)
                .header("Authorization", MERCHANT_PHONE))
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/order/complete/" + orderId)
                .header("Authorization", USER_PHONE))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/review/add")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", USER_PHONE,
                    "score", 5,
                    "content", "集成测试评价"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.score").value(5));

        mockMvc.perform(get("/api/review/rating/1"))
            .andExpect(jsonPath("$.data.reviewCount").value(1))
            .andExpect(jsonPath("$.data.averageScore").value(5.0));

        mockMvc.perform(post("/api/review/add")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", USER_PHONE,
                    "score", 3,
                    "content", "重复评价"
                ))))
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createAnnouncementWorks() throws Exception {
        mockMvc.perform(post("/api/announcement/create")
                .header("Authorization", ADMIN_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "新公告",
                    "content", "公告内容"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("新公告"));
    }

    @Test
    void bookingFlowRequiresOwnerAndMerchantRoles() throws Exception {
        mockMvc.perform(get("/api/bookings/merchant")
                .header("Authorization", USER_PHONE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "merchantId", 1,
                    "serviceName", "到店咨询",
                    "appointmentTime", LocalDateTime.now().plusDays(2),
                    "contactPhone", USER_PHONE,
                    "note", "集成测试预约"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.userId").value(USER_PHONE))
            .andReturn();

        Long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        mockMvc.perform(post("/api/bookings/" + bookingId + "/status")
                .header("Authorization", USER_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/bookings/" + bookingId + "/status")
                .header("Authorization", MERCHANT_PHONE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                .header("Authorization", USER_PHONE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }
}
