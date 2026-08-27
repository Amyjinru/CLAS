package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.UserContext;
import com.clas.dto.LoginRequest;
import com.clas.dto.LoginNoticeResponse;
import com.clas.dto.LoginResponse;
import com.clas.dto.RegisterRequest;
import com.clas.dto.ResetPasswordRequest;
import com.clas.config.RequireRole;
import com.clas.dto.SendCodeRequest;
import com.clas.entity.User;
import com.clas.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    /**
     * 注册 — 发送验证码到手机号。
     * 检查手机号未被注册，生成验证码存入 VerificationCodeStore。
     */
    @PostMapping("/register/send-code")
    public Result<String> sendRegisterCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendRegisterCode(request);
        return Result.ok("验证码已发送");
    }

    /**
     * 忘记密码 — 发送验证码到已绑定手机号。
     */
    @PostMapping("/forgot-password/send-code")
    public Result<String> sendForgotPasswordCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendForgotPasswordCode(request);
        return Result.ok("验证码已发送");
    }

    @PostMapping("/login/send-code")
    public Result<String> sendLoginCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendLoginCode(request);
        return Result.ok("验证码已发送");
    }

    @GetMapping("/login-notice")
    @RequireRole({"USER", "MERCHANT", "ADMIN", "RIDER"})
    public Result<LoginNoticeResponse> loginNotice() {
        User user = UserContext.getUser();
        return Result.ok(user == null ? null : userService.getPendingLoginNotice(user.getPhone(), user.getSessionToken()));
    }

    /**
     * 忘记密码 — 验证码校验 + 密码重置 + 自动登录。
     */
    @PostMapping("/forgot-password/reset")
    public Result<LoginResponse> resetForgotPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return Result.ok(userService.resetForgotPassword(request));
    }

    @PostMapping("/switch-role")
    @RequireRole({"USER", "MERCHANT", "ADMIN", "RIDER"})
    public Result<LoginResponse> switchRole(@Valid @RequestBody com.clas.dto.RoleSwitchRequest request) {
        return Result.ok(userService.switchRole(UserContext.getUserId(), request.role()));
    }

    /**
     * 退出登录：清除服务端会话，使该账号其它设备可免验证码直接登录。
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        User user = UserContext.getUser();
        if (user != null) {
            userService.logout(user.getPhone(), user.getSessionToken());
        }
        return Result.ok("已退出登录");
    }
}
