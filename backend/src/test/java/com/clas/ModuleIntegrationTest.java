package com.clas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.clas.common.BusinessException;
import com.clas.entity.Coupon;
import com.clas.entity.Notification;
import com.clas.entity.Product;
import com.clas.entity.Review;
import com.clas.entity.ReviewImage;
import com.clas.entity.ReviewReply;
import com.clas.entity.ReviewVote;
import com.clas.entity.UserCoupon;
import com.clas.mapper.CouponMapper;
import com.clas.mapper.NotificationMapper;
import com.clas.mapper.ProductMapper;
import com.clas.mapper.ReviewImageMapper;
import com.clas.mapper.ReviewMapper;
import com.clas.mapper.ReviewReplyMapper;
import com.clas.mapper.ReviewVoteMapper;
import com.clas.mapper.UserCouponMapper;
import com.clas.service.CouponService;
import com.clas.service.OrderTimeoutService;
import java.util.Base64;
import java.util.Properties;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
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

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private CouponService couponService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private ReviewImageMapper reviewImageMapper;

    @Autowired
    private ReviewReplyMapper reviewReplyMapper;

    @Autowired
    private ReviewVoteMapper reviewVoteMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private OrderTimeoutService orderTimeoutService;

    @Test
    void apiResponsesIncludeTimestampAndRequestId() throws Exception {
        mockMvc.perform(get("/api/health")
                .header("X-Request-Id", "trace-test-123"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Request-Id", "trace-test-123"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.timestamp").isNumber())
            .andExpect(jsonPath("$.requestId").value("trace-test-123"));
    }

    @Test
    void apiErrorsIncludeRequestId() throws Exception {
        mockMvc.perform(get("/api/deals/999999")
                .header("X-Request-Id", "missing-deal-trace"))
            .andExpect(status().isBadRequest())
            .andExpect(header().string("X-Request-Id", "missing-deal-trace"))
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("团购券不存在"))
            .andExpect(jsonPath("$.timestamp").isNumber())
            .andExpect(jsonPath("$.requestId").value("missing-deal-trace"))
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

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
            .andExpect(jsonPath("$.data.user.username").value("new_user"))
            .andExpect(jsonPath("$.data.user.role").value("USER"))
            .andExpect(jsonPath("$.data.user.password").doesNotExist());
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
            .andExpect(jsonPath("$.data.user.phone").value("13900000011"))
            .andExpect(jsonPath("$.data.user.username").value("user"));
    }

    @Test
    void userRegisterRejectsDuplicatePhone() throws Exception {
        // 手机号也要保持唯一，避免不同账号共享同一份身份资料。
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", USER_PHONE))))
            .andExpect(status().isBadRequest())
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
            .andExpect(status().isBadRequest())
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
            .andExpect(status().isBadRequest())
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
            .andExpect(status().isBadRequest())
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
    void merchantCanChangeBoundLoginPhoneAndKeepMerchantProfile() throws Exception {
        String oldPhone = "13900000032";
        String newPhone = "13900000033";

        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", oldPhone))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/merchant/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.ofEntries(
                    Map.entry("accountPhone", oldPhone),
                    Map.entry("code", TEST_CODE),
                    Map.entry("username", "merchant_phone_change"),
                    Map.entry("password", STRONG_PASSWORD),
                    Map.entry("confirmPassword", STRONG_PASSWORD),
                    Map.entry("merchantName", "改绑测试商家"),
                    Map.entry("contactPhone", "13900000034"),
                    Map.entry("category", "美食"),
                    Map.entry("address", "测试地址 2 号"),
                    Map.entry("longitude", 116.390000),
                    Map.entry("latitude", 39.910000),
                    Map.entry("deliveryRadiusM", 3000),
                    Map.entry("bankAccount", "123456780"),
                    Map.entry("settlementCycle", 7)
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(oldPhone));

        String token = loginToken(oldPhone);

        mockMvc.perform(post("/api/user/phone-change/send-code")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", newPhone))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        MvcResult changeResult = mockMvc.perform(put("/api/user/phone")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", newPhone,
                    "code", TEST_CODE
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.phone").value(newPhone))
            .andReturn();
        String newToken = objectMapper.readTree(changeResult.getResponse().getContentAsString())
            .path("data").path("token").asText();

        mockMvc.perform(get("/api/merchant/my")
                .header("Authorization", "Bearer " + newToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(newPhone))
            .andExpect(jsonPath("$.data.phone").value("13900000034"));
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("手机号或密码错误"));
    }

    @Test
    void userCanChangeBoundPhoneWithVerificationCode() throws Exception {
        String oldPhone = "13900000030";
        String newPhone = "13900000031";

        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", oldPhone))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        MvcResult registerResult = mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "phone_change_user",
                    "password", STRONG_PASSWORD,
                    "confirmPassword", STRONG_PASSWORD,
                    "phone", oldPhone,
                    "code", TEST_CODE
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").isString())
            .andReturn();
        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
            .path("data").path("token").asText();

        mockMvc.perform(post("/api/user/phone-change/send-code")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", newPhone))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        MvcResult changeResult = mockMvc.perform(put("/api/user/phone")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", newPhone,
                    "code", TEST_CODE
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.user.phone").value(newPhone))
            .andExpect(jsonPath("$.data.user.password").doesNotExist())
            .andExpect(jsonPath("$.data.token").isString())
            .andReturn();

        String newToken = objectMapper.readTree(changeResult.getResponse().getContentAsString())
            .path("data").path("token").asText();

        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + newToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.phone").value(newPhone));

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", oldPhone,
                    "password", STRONG_PASSWORD
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("手机号或密码错误"));

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", newPhone,
                    "password", STRONG_PASSWORD
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.phone").value(newPhone));
    }

    @Test
    void userCanChangePasswordWithCurrentPassword() throws Exception {
        String phone = "13900000040";
        String nextPassword = "Abc123!!";
        registerUser(phone, "password_change_user", STRONG_PASSWORD);
        String token = loginToken(phone);

        mockMvc.perform(put("/api/user/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "currentPassword", STRONG_PASSWORD,
                    "newPassword", nextPassword,
                    "confirmPassword", nextPassword
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", phone,
                    "password", STRONG_PASSWORD
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("手机号或密码错误"));

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", phone,
                    "password", nextPassword
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.user.phone").value(phone));
    }

    @Test
    void passwordChangeRejectsWrongCurrentPasswordAndMismatch() throws Exception {
        String phone = "13900000041";
        registerUser(phone, "password_reject_user", STRONG_PASSWORD);
        String token = loginToken(phone);

        mockMvc.perform(put("/api/user/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "currentPassword", "Wrong123!",
                    "newPassword", "Abc123!!",
                    "confirmPassword", "Abc123!!"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("当前密码错误"));

        mockMvc.perform(put("/api/user/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "currentPassword", STRONG_PASSWORD,
                    "newPassword", "Abc123!!",
                    "confirmPassword", "Abc123!!!"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("两次输入的密码不一致"));
    }

    @Test
    void userBankCardsSupportMultipleMaskedCardsAndOwnerDelete() throws Exception {
        String token = auth(USER_PHONE);

        MvcResult first = mockMvc.perform(post("/api/user/bank-cards")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "bankName", "招商银行",
                    "cardholderName", "测试用户",
                    "cardNo", "6225881234567890",
                    "cardType", "借记卡",
                    "isDefault", true
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.maskedCardNo").value("**** **** **** 7890"))
            .andExpect(jsonPath("$.data.cardNo").doesNotExist())
            .andExpect(jsonPath("$.data.isDefault").value(true))
            .andReturn();

        mockMvc.perform(post("/api/user/bank-cards")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "bankName", "中国银行",
                    "cardholderName", "测试用户",
                    "cardNo", "6217000000001234",
                    "cardType", "信用卡",
                    "isDefault", false
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.maskedCardNo").value("**** **** **** 1234"));

        mockMvc.perform(get("/api/user/bank-cards")
                .header("Authorization", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].maskedCardNo").isString())
            .andExpect(jsonPath("$.data[0].cardNo").doesNotExist());

        jdbcTemplate.update("""
            INSERT INTO user_bank_card(user_id, bank_name, cardholder_name, card_no_encrypted, card_last4, card_type, is_default, create_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """, ADMIN_PHONE, "他人银行", "他人", "**** **** **** 9999", "9999", "借记卡", false);
        Long otherCardId = jdbcTemplate.queryForObject("SELECT id FROM user_bank_card WHERE user_id = ?", Long.class, ADMIN_PHONE);

        mockMvc.perform(delete("/api/user/bank-cards/" + otherCardId)
                .header("Authorization", token))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("银行卡不存在或无权操作"));

        Long firstCardId = objectMapper.readTree(first.getResponse().getContentAsString())
            .path("data").path("id").asLong();
        mockMvc.perform(delete("/api/user/bank-cards/" + firstCardId)
                .header("Authorization", token))
            .andExpect(status().isOk());
    }

    @Test
    void adminMerchantListRequiresAdminRole() throws Exception {
        // 管理员接口必须同时拦截未登录用户和非 ADMIN 角色用户。
        String userToken = loginToken(USER_PHONE);
        String adminToken = loginToken(ADMIN_PHONE);

        mockMvc.perform(get("/api/merchant/admin/list"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录，请先登录"));

        mockMvc.perform(get("/api/merchant/admin/list")
                .header("Authorization", USER_PHONE))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(get("/api/merchant/admin/list")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("权限不足，无法访问"));

        mockMvc.perform(get("/api/merchant/admin/list")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void merchantListCalculatesDistanceFromAddress() throws Exception {
        mockMvc.perform(get("/api/merchant/list")
                .header("Authorization", auth(USER_PHONE))
                .param("addressId", "1")
                .param("sort", "distance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].distanceMeters").isNumber())
            .andExpect(jsonPath("$.data[0].estimatedMinutes").isNumber())
            .andExpect(jsonPath("$.data[0].deliveryAvailable").value(true));
    }

    @Test
    void merchantLogoUploadUpdatesCurrentMerchant() throws Exception {
        MockMultipartFile logo = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
            )
        );

        mockMvc.perform(multipart("/api/merchant/my/logo")
                .file(logo)
                .header("Authorization", auth(MERCHANT_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.logo").isString())
            .andExpect(jsonPath("$.data.logo").value(org.hamcrest.Matchers.startsWith("/uploads/merchant-logo/1/")));
    }

    @Test
    void merchantProfileUpdateRequiresCodeForSensitiveFields() throws Exception {
        Map<String, Object> payload = Map.of(
            "merchantName", "Campus Light Meals Updated",
            "address", "Software Park West Gate No.2",
            "longitude", 116.398000,
            "latitude", 39.910000,
            "deliveryRadiusM", 3500,
            "phone", "13900009999",
            "bankAccount", "6222000000000000099",
            "code", TEST_CODE
        );

        mockMvc.perform(post("/api/merchant/my/profile/send-code")
                .header("Authorization", auth(MERCHANT_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(put("/api/merchant/my/profile")
                .header("Authorization", auth(MERCHANT_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.merchantName").value("Campus Light Meals Updated"))
            .andExpect(jsonPath("$.data.phone").value("13900009999"))
            .andExpect(jsonPath("$.data.bankAccount").value("6222000000000000099"))
            .andExpect(jsonPath("$.data.address").value("Software Park West Gate No.2"));
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
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/api/cart/update")
                .header("Authorization", auth(USER_PHONE))
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
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void canonicalCurrentUserRoutesIgnoreClientSuppliedIds() throws Exception {
        String otherUserId = "13900009999";

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", otherUserId,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].userId").value(USER_PHONE));

        mockMvc.perform(get("/api/cart/me")
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].userId").value(USER_PHONE));

        mockMvc.perform(get("/api/cart/list/" + otherUserId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].userId").value(USER_PHONE));

        MvcResult orderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", otherUserId,
                    "merchantId", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.userId").value(USER_PHONE))
            .andReturn();

        mockMvc.perform(get("/api/order/me")
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/order/list/" + otherUserId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();
        mockMvc.perform(post("/api/order/cancel/" + orderId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/order/merchant/999")
                .header("Authorization", auth(MERCHANT_PHONE)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/order/merchant/me")
                .header("Authorization", auth(MERCHANT_PHONE)))
            .andExpect(status().isOk());
    }

    @Test
    void orderDetailIsScopedToCurrentUserMerchantAndAdmin() throws Exception {
        String otherUserId = "13900008888";
        String otherMerchantPhone = "13900008889";
        registerUser(otherUserId, "order_detail_other", STRONG_PASSWORD);
        registerMerchant(otherMerchantPhone, "order_detail_merchant", "详情测试商家", "13900008890");

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk());

        MvcResult orderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "merchantId", 1
                ))))
            .andExpect(status().isOk())
            .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();

        mockMvc.perform(get("/api/order/" + orderId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.id").value(orderId))
            .andExpect(jsonPath("$.data.items.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/order/" + orderId)
                .header("Authorization", auth(otherUserId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("AUTH_FORBIDDEN"));

        mockMvc.perform(get("/api/order/merchant/detail/" + orderId)
                .header("Authorization", auth(MERCHANT_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.id").value(orderId))
            .andExpect(jsonPath("$.data.items.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/order/merchant/detail/" + orderId)
                .header("Authorization", auth(otherMerchantPhone)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("AUTH_FORBIDDEN"));

        mockMvc.perform(get("/api/order/admin/" + orderId)
                .header("Authorization", auth(ADMIN_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.id").value(orderId));
    }

    @Test
    void cancelPendingOrderRestoresStock() throws Exception {
        MvcResult beforeStockResult = mockMvc.perform(get("/api/product/list/1"))
            .andExpect(status().isOk())
            .andReturn();
        int beforeStock = objectMapper.readTree(beforeStockResult.getResponse().getContentAsString())
            .path("data").get(0).path("stock").asInt();

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk());

        MvcResult orderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", auth(USER_PHONE))
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
            .andExpect(jsonPath("$.data[0].stock").value(beforeStock));

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();

        mockMvc.perform(post("/api/order/cancel/" + orderId)
                .header("Authorization", auth(USER_PHONE)))
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
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        MvcResult orderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "merchantId", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.status").value("PENDING_PAYMENT"))
            .andReturn();

        mockMvc.perform(get("/api/product/list/1"))
            .andExpect(jsonPath("$.data[0].stock").value(30));

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();

        mockMvc.perform(get("/api/payment/status/" + orderId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.orderStatus").value("PENDING_PAYMENT"));

        mockMvc.perform(post("/api/payment/mock")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", USER_PHONE,
                    "payMethod", "MOCK"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        mockMvc.perform(get("/api/product/list/1"))
            .andExpect(jsonPath("$.data[0].stock").value(29));

        mockMvc.perform(post("/api/payment/mock")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", USER_PHONE,
                    "payMethod", "MOCK"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"));

        mockMvc.perform(get("/api/product/list/1"))
            .andExpect(jsonPath("$.data[0].stock").value(29));

        mockMvc.perform(post("/api/order/accept/" + orderId)
                .header("Authorization", auth(MERCHANT_PHONE)))
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/order/complete/" + orderId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/review/add")
                .header("Authorization", auth(USER_PHONE))
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
                .header("Authorization", auth(USER_PHONE))
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
    void orderLifecycleTimestampsProgressThroughDetail() throws Exception {
        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk());

        MvcResult orderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "merchantId", 1
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.createTime").isString())
            .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();

        mockMvc.perform(post("/api/order/pay/" + orderId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        mockMvc.perform(post("/api/order/accept/" + orderId)
                .header("Authorization", auth(MERCHANT_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.acceptedAt").isString());

        mockMvc.perform(post("/api/order/deliver/" + orderId)
                .header("Authorization", auth(MERCHANT_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.deliveredAt").isString());

        mockMvc.perform(post("/api/order/complete/" + orderId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.completedAt").isString());

        mockMvc.perform(get("/api/order/" + orderId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.order.paidAt").isString())
            .andExpect(jsonPath("$.data.order.acceptedAt").isString())
            .andExpect(jsonPath("$.data.order.deliveredAt").isString())
            .andExpect(jsonPath("$.data.order.completedAt").isString());
    }

    @Test
    void paymentIdempotencyKeyReusesSamePayment() throws Exception {
        Long orderId = createPendingOrderForUser();
        String key = "payment-key-" + orderId;

        MvcResult firstPay = mockMvc.perform(post("/api/payment/mock")
                .header("Authorization", auth(USER_PHONE))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", USER_PHONE,
                    "payMethod", "MOCK"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderStatus").value("PAID"))
            .andExpect(jsonPath("$.data.idempotencyKey").value(key))
            .andReturn();

        Long firstPaymentId = objectMapper.readTree(firstPay.getResponse().getContentAsString())
            .path("data").path("paymentId").asLong();

        MvcResult secondPay = mockMvc.perform(post("/api/payment/mock")
                .header("Authorization", auth(USER_PHONE))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", USER_PHONE,
                    "payMethod", "MOCK"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("SUCCESS"))
            .andExpect(jsonPath("$.data.orderStatus").value("PAID"))
            .andExpect(jsonPath("$.data.idempotencyKey").value(key))
            .andReturn();

        Long secondPaymentId = objectMapper.readTree(secondPay.getResponse().getContentAsString())
            .path("data").path("paymentId").asLong();
        assertEquals(firstPaymentId, secondPaymentId);
    }

    @Test
    void paymentIdempotencyKeyCannotBeReusedForAnotherOrder() throws Exception {
        Long firstOrderId = createPendingOrderForUser();
        String key = "reused-payment-key";

        mockMvc.perform(post("/api/payment/mock")
                .header("Authorization", auth(USER_PHONE))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", firstOrderId,
                    "userId", USER_PHONE,
                    "payMethod", "MOCK"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.idempotencyKey").value(key));

        Long secondOrderId = createPendingOrderForUser();

        mockMvc.perform(post("/api/payment/mock")
                .header("Authorization", auth(USER_PHONE))
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", secondOrderId,
                    "userId", USER_PHONE,
                    "payMethod", "MOCK"
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("幂等键已用于其他订单"))
            .andExpect(jsonPath("$.errorCode").value("PAYMENT_IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void pendingPaymentTimeoutCancelsOldOrders() throws Exception {
        Long orderId = createPendingOrderForUser();
        jdbcTemplate.update(
            "UPDATE orders SET create_time = ? WHERE id = ?",
            LocalDateTime.now().minusMinutes(31),
            orderId
        );

        int expired = orderTimeoutService.expirePendingPaymentOrders(LocalDateTime.now());
        assertEquals(1, expired);

        mockMvc.perform(get("/api/payment/status/" + orderId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("CANCELED"))
            .andExpect(jsonPath("$.data.paymentStatus").value("FAILED"));

        Notification notification = notificationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                    .eq(Notification::getUserId, USER_PHONE)
                    .eq(Notification::getOrderId, orderId)
                    .eq(Notification::getType, "ORDER_STATUS")
                    .orderByDesc(Notification::getId)
            )
            .stream()
            .findFirst()
            .orElseThrow();
        assertEquals("订单已超时取消", notification.getTitle());
    }

    @Test
    void reviewCommentCreatesReplyNotificationAndLegacyNotificationsRemainValid() throws Exception {
        Long orderId = createCompletedOrder();

        MvcResult reviewResult = mockMvc.perform(post("/api/review/add")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", USER_PHONE,
                    "score", 5,
                    "content", "等待用户评论回复"
                ))))
            .andExpect(status().isOk())
            .andReturn();
        Long reviewId = objectMapper.readTree(reviewResult.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        MvcResult replyResult = mockMvc.perform(post("/api/review/" + reviewId + "/comments")
                .header("Authorization", auth(ADMIN_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "我来补充一条评论"))))
            .andExpect(status().isOk())
            .andReturn();
        Long replyId = objectMapper.readTree(replyResult.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        Notification replyNotification = notificationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                    .eq(Notification::getUserId, USER_PHONE)
                    .eq(Notification::getType, "REVIEW_REPLY")
                    .eq(Notification::getReplyId, replyId)
                    .orderByDesc(Notification::getId)
            )
            .stream()
            .findFirst()
            .orElseThrow();

        assertEquals("REPLY", replyNotification.getTargetType());
        assertEquals(replyId, replyNotification.getTargetId());
        assertEquals(reviewId, replyNotification.getReviewId());
        assertEquals(orderId, replyNotification.getOrderId());
        assertEquals(1L, replyNotification.getMerchantId());
        assertEquals("/review/" + orderId + "?reviewId=" + reviewId + "&replyId=" + replyId, replyNotification.getTargetPath());

        mockMvc.perform(get("/api/notifications/mine")
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").exists());
    }

    @Test
    void couponIsReservedReleasedAndUsedAcrossOrderLifecycle() throws Exception {
        MvcResult claimResult = mockMvc.perform(post("/api/coupon/claim/1")
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("UNUSED"))
            .andReturn();
        Long userCouponId = objectMapper.readTree(claimResult.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk());

        MvcResult canceledOrderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "merchantId", 1,
                    "userCouponId", userCouponId
                ))))
            .andExpect(status().isOk())
            .andReturn();
        Long canceledOrderId = objectMapper.readTree(canceledOrderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();

        UserCoupon reserved = userCouponMapper.selectById(userCouponId);
        assertEquals("RESERVED", reserved.getStatus());
        assertEquals(canceledOrderId, reserved.getOrderId());

        mockMvc.perform(post("/api/order/cancel/" + canceledOrderId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELED"));

        UserCoupon released = userCouponMapper.selectById(userCouponId);
        assertEquals("UNUSED", released.getStatus());
        assertEquals(null, released.getOrderId());

        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk());

        MvcResult paidOrderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "merchantId", 1,
                    "userCouponId", userCouponId
                ))))
            .andExpect(status().isOk())
            .andReturn();
        Long paidOrderId = objectMapper.readTree(paidOrderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();

        mockMvc.perform(post("/api/payment/mock")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", paidOrderId,
                    "userId", USER_PHONE,
                    "payMethod", "MOCK"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        UserCoupon used = userCouponMapper.selectById(userCouponId);
        assertEquals("USED", used.getStatus());
        assertEquals(paidOrderId, used.getOrderId());
    }

    @Test
    void limitedCouponCannotBeOverClaimed() {
        Coupon coupon = new Coupon();
        coupon.setTitle("Single claim coupon");
        coupon.setDescription("Limit regression");
        coupon.setCouponType("FIXED");
        coupon.setDiscountAmount(100);
        coupon.setDiscountPercent(null);
        coupon.setMinOrderAmount(0);
        coupon.setMerchantId(null);
        coupon.setTotalLimit(1);
        coupon.setClaimedCount(0);
        coupon.setValidFrom(LocalDateTime.now().minusDays(1));
        coupon.setValidTo(LocalDateTime.now().plusDays(1));
        coupon.setStatus("ACTIVE");
        coupon.setCreatedAt(LocalDateTime.now());
        couponMapper.insert(coupon);

        couponService.claim(USER_PHONE, coupon.getId());

        assertThrows(BusinessException.class, () -> couponService.claim("13900000999", coupon.getId()));
        assertEquals(1, couponMapper.selectById(coupon.getId()).getClaimedCount());
    }

    @Test
    void paymentFailsWithoutMarkingOrderPaidWhenStockRunsOutAfterOrderCreation() throws Exception {
        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk());

        MvcResult orderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "merchantId", 1
                ))))
            .andExpect(status().isOk())
            .andReturn();
        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();

        Product product = productMapper.selectById(1L);
        int originalStock = product.getStock();
        product.setStock(0);
        productMapper.updateById(product);
        try {
            mockMvc.perform(post("/api/payment/mock")
                    .header("Authorization", auth(USER_PHONE))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "orderId", orderId,
                        "userId", USER_PHONE,
                        "payMethod", "MOCK"
                    ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("库存不足：Chicken Energy Bowl"));

            mockMvc.perform(get("/api/payment/status/" + orderId)
                    .header("Authorization", auth(USER_PHONE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.orderStatus").value("PENDING_PAYMENT"));
        } finally {
            product.setStock(originalStock);
            productMapper.updateById(product);
        }
    }

    @Test
    void orderAndReviewListsUseBatchedDetailQueries() throws Exception {
        Long firstOrderId = createPendingOrderForUser();
        Long secondOrderId = createPendingOrderForUser();
        insertReviewFixture(firstOrderId, "批量评价 1", 1);
        insertReviewFixture(secondOrderId, "批量评价 2", 2);

        String userAuth = auth(USER_PHONE);

        MybatisQueryCounter.reset();
        mockMvc.perform(get("/api/order/list/" + USER_PHONE)
                .header("Authorization", userAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        org.junit.jupiter.api.Assertions.assertTrue(
            MybatisQueryCounter.count() <= 4,
            "订单列表应批量加载明细，当前查询次数: " + MybatisQueryCounter.count()
        );

        MybatisQueryCounter.reset();
        mockMvc.perform(get("/api/review/mine")
                .header("Authorization", userAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        org.junit.jupiter.api.Assertions.assertTrue(
            MybatisQueryCounter.count() <= 8,
            "评价列表应批量加载用户、图片、回复和投票，当前查询次数: " + MybatisQueryCounter.count()
        );
    }

    @Test
    void databaseRejectsCoreOrphanRows() {
        long validOrderId = 99001L;
        jdbcTemplate.update("""
            INSERT INTO orders (
                id, user_id, merchant_id, total_price, subtotal, delivery_fee, coupon_discount,
                status, delivery_status, estimated_minutes, create_time
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            validOrderId, USER_PHONE, 1L, 100, 100, 0, 0,
            "PENDING_PAYMENT", "WAITING", 30, LocalDateTime.now()
        );

        assertThrows(Exception.class, () -> jdbcTemplate.update(
            "INSERT INTO order_item (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)",
            -9001L, 1L, 1, 100
        ));
        assertThrows(Exception.class, () -> jdbcTemplate.update(
            "INSERT INTO order_item (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)",
            validOrderId, -9001L, 1, 100
        ));
        assertThrows(Exception.class, () -> jdbcTemplate.update(
            "INSERT INTO payment (order_id, user_id, amount, pay_method, status, create_time) VALUES (?, ?, ?, ?, ?, ?)",
            -9001L, USER_PHONE, 100, "MOCK", "PENDING", LocalDateTime.now()
        ));
        assertThrows(Exception.class, () -> jdbcTemplate.update(
            "INSERT INTO review (order_id, user_id, score, content, report_status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            -9001L, USER_PHONE, 5, "orphan", "NONE", LocalDateTime.now()
        ));
        assertThrows(Exception.class, () -> jdbcTemplate.update(
            "INSERT INTO user_coupon (user_id, coupon_id, status, claimed_at) VALUES (?, ?, ?, ?)",
            USER_PHONE, -9001L, "UNUSED", LocalDateTime.now()
        ));
    }

    @Test
    void createAnnouncementWorks() throws Exception {
        mockMvc.perform(post("/api/announcement/create")
                .header("Authorization", auth(ADMIN_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "title", "新公告",
                    "content", "公告内容"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("新公告"));
    }

    @Test
    void groupDealDetailReturnsExistingDeal() throws Exception {
        mockMvc.perform(get("/api/deals/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.title").exists())
            .andExpect(jsonPath("$.data.merchantId").value(1));
    }

    @Test
    void groupDealDetailRejectsMissingDeal() throws Exception {
        mockMvc.perform(get("/api/deals/999999"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("团购券不存在"));
    }

    @Test
    void buyingGroupDealCreatesClickableDealOrderNotification() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/deals/1/buy")
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn();

        Long dealOrderId = objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        Notification notification = notificationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Notification>()
                    .eq(Notification::getUserId, USER_PHONE)
                    .eq(Notification::getType, "DEAL_ORDER_STATUS")
                    .eq(Notification::getTargetId, dealOrderId)
                    .orderByDesc(Notification::getId)
            )
            .stream()
            .findFirst()
            .orElseThrow();

        assertEquals("DEAL_ORDER", notification.getTargetType());
        assertEquals(dealOrderId, notification.getOrderId());
        assertEquals(1L, notification.getMerchantId());
        assertEquals("/deal-order/" + dealOrderId, notification.getTargetPath());
    }

    @Test
    void bookingFlowRequiresOwnerAndMerchantRoles() throws Exception {
        mockMvc.perform(get("/api/bookings/merchant")
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                .header("Authorization", auth(USER_PHONE))
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
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/api/bookings/" + bookingId + "/status")
                .header("Authorization", auth(MERCHANT_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }

    private Long createPendingOrderForUser() throws Exception {
        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "productId", 1,
                    "quantity", 1
                ))))
            .andExpect(status().isOk());

        MvcResult orderResult = mockMvc.perform(post("/api/order/create")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "userId", USER_PHONE,
                    "merchantId", 1
                ))))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(orderResult.getResponse().getContentAsString())
            .path("data").path("order").path("id").asLong();
    }

    private Long createCompletedOrder() throws Exception {
        Long orderId = createPendingOrderForUser();
        mockMvc.perform(post("/api/payment/mock")
                .header("Authorization", auth(USER_PHONE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "orderId", orderId,
                    "userId", USER_PHONE,
                    "payMethod", "MOCK"
                ))))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/order/accept/" + orderId)
                .header("Authorization", auth(MERCHANT_PHONE)))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/order/complete/" + orderId)
                .header("Authorization", auth(USER_PHONE)))
            .andExpect(status().isOk());
        return orderId;
    }

    private void insertReviewFixture(Long orderId, String content, int sortOrder) {
        Review review = new Review();
        review.setOrderId(orderId);
        review.setUserId(USER_PHONE);
        review.setScore(5);
        review.setContent(content);
        review.setReportStatus("NONE");
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);

        ReviewImage image = new ReviewImage();
        image.setReviewId(review.getId());
        image.setImageUrl("https://example.test/review-" + review.getId() + ".jpg");
        image.setSortOrder(sortOrder);
        image.setCreatedAt(LocalDateTime.now());
        reviewImageMapper.insert(image);

        ReviewReply reply = new ReviewReply();
        reply.setReviewId(review.getId());
        reply.setUserId(MERCHANT_PHONE);
        reply.setReplyType("MERCHANT");
        reply.setContent("谢谢反馈 " + review.getId());
        reply.setDeleted(false);
        reply.setCreatedAt(LocalDateTime.now());
        reviewReplyMapper.insert(reply);

        ReviewVote reviewVote = new ReviewVote();
        reviewVote.setTargetType("REVIEW");
        reviewVote.setTargetId(review.getId());
        reviewVote.setUserId(ADMIN_PHONE);
        reviewVote.setVoteType("LIKE");
        reviewVote.setCreatedAt(LocalDateTime.now());
        reviewVoteMapper.insert(reviewVote);

        ReviewVote replyVote = new ReviewVote();
        replyVote.setTargetType("REPLY");
        replyVote.setTargetId(reply.getId());
        replyVote.setUserId(USER_PHONE);
        replyVote.setVoteType("LIKE");
        replyVote.setCreatedAt(LocalDateTime.now());
        reviewVoteMapper.insert(replyVote);
    }

    private String loginToken(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "phone", phone,
                    "password", STRONG_PASSWORD
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").isString())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data").path("token").asText();
    }

    private String auth(String phone) throws Exception {
        return "Bearer " + loginToken(phone);
    }

    private void registerUser(String phone, String username, String password) throws Exception {
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", phone))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", username,
                    "password", password,
                    "confirmPassword", password,
                    "phone", phone,
                    "code", TEST_CODE
                ))))
            .andExpect(status().isOk());
    }

    private void registerMerchant(
        String accountPhone,
        String username,
        String merchantName,
        String contactPhone
    ) throws Exception {
        mockMvc.perform(post("/api/user/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", accountPhone))))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/merchant/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.ofEntries(
                    Map.entry("accountPhone", accountPhone),
                    Map.entry("code", TEST_CODE),
                    Map.entry("username", username),
                    Map.entry("password", STRONG_PASSWORD),
                    Map.entry("confirmPassword", STRONG_PASSWORD),
                    Map.entry("merchantName", merchantName),
                    Map.entry("contactPhone", contactPhone),
                    Map.entry("category", "美食"),
                    Map.entry("address", "测试地址 3 号"),
                    Map.entry("longitude", 116.390000),
                    Map.entry("latitude", 39.910000),
                    Map.entry("deliveryRadiusM", 3000),
                    Map.entry("bankAccount", "123456781"),
                    Map.entry("settlementCycle", 7)
                ))))
            .andExpect(status().isOk());
    }

    @TestConfiguration
    static class MybatisQueryCounterConfig {
        @Bean
        MybatisQueryCounter mybatisQueryCounter() {
            return new MybatisQueryCounter();
        }
    }

    @Intercepts({
        @Signature(type = Executor.class, method = "query", args = {
            MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
        }),
        @Signature(type = Executor.class, method = "query", args = {
            MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class
        })
    })
    static class MybatisQueryCounter implements Interceptor {
        private static final AtomicInteger QUERIES = new AtomicInteger();

        static void reset() {
            QUERIES.set(0);
        }

        static int count() {
            return QUERIES.get();
        }

        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            QUERIES.incrementAndGet();
            return invocation.proceed();
        }

        @Override
        public Object plugin(Object target) {
            return Plugin.wrap(target, this);
        }

        @Override
        public void setProperties(Properties properties) {
        }
    }
}
