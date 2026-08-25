package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.RoleApplicationAuditRequest;
import com.clas.dto.RoleApplicationCreateRequest;
import com.clas.entity.RoleApplication;
import com.clas.entity.User;
import com.clas.mapper.RoleApplicationMapper;
import com.clas.mapper.UserMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleApplicationService {
    private static final String RIDER = "RIDER";
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final RoleApplicationMapper roleApplicationMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final UserService userService;

    public RoleApplicationService(
        RoleApplicationMapper roleApplicationMapper,
        UserMapper userMapper,
        NotificationService notificationService,
        UserService userService
    ) {
        this.roleApplicationMapper = roleApplicationMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @Transactional
    public RoleApplication applyForRider(String userId, RoleApplicationCreateRequest request) {
        User user = requireUser(userId);
        if (!"USER".equals(user.getRole())) {
            throw new BusinessException("只有普通用户可以申请骑手身份");
        }
        if (userService.rolesOf(userId).stream().anyMatch(role -> RIDER.equals(role) || "MERCHANT".equals(role))) {
            throw new BusinessException("已拥有商家或骑手身份，不能申请其他业务身份");
        }
        boolean pending = roleApplicationMapper.exists(new LambdaQueryWrapper<RoleApplication>()
            .eq(RoleApplication::getUserId, userId)
            .eq(RoleApplication::getTargetRole, RIDER)
            .eq(RoleApplication::getStatus, PENDING));
        if (pending) {
            throw new BusinessException("已有待审核的骑手申请");
        }

        RoleApplication application = new RoleApplication();
        application.setUserId(userId);
        application.setTargetRole(RIDER);
        application.setReason(request.reason().trim());
        application.setStatus(PENDING);
        roleApplicationMapper.insert(application);
        notificationService.notifyAdmins("新的骑手申请", "用户 " + userId + " 提交了骑手身份申请。");
        return application;
    }

    public List<RoleApplication> listMine(String userId) {
        return roleApplicationMapper.selectList(new LambdaQueryWrapper<RoleApplication>()
            .eq(RoleApplication::getUserId, userId)
            .orderByDesc(RoleApplication::getCreatedAt));
    }

    public List<RoleApplication> listForAdmin(String status) {
        LambdaQueryWrapper<RoleApplication> query = new LambdaQueryWrapper<RoleApplication>()
            .orderByDesc(RoleApplication::getCreatedAt);
        if (status != null && !status.isBlank()) {
            query.eq(RoleApplication::getStatus, status.trim().toUpperCase());
        }
        return roleApplicationMapper.selectList(query);
    }

    @Transactional
    public RoleApplication audit(Long id, RoleApplicationAuditRequest request, String adminId) {
        RoleApplication application = roleApplicationMapper.selectById(id);
        if (application == null) {
            throw new BusinessException("身份申请不存在");
        }
        if (!PENDING.equals(application.getStatus())) {
            throw new BusinessException("只能审核待处理的身份申请");
        }
        String status = request.status().trim().toUpperCase();
        if (!APPROVED.equals(status) && !REJECTED.equals(status)) {
            throw new BusinessException("审核结果只能是 APPROVED 或 REJECTED");
        }
        if (APPROVED.equals(status)) {
            User applicant = requireUser(application.getUserId());
            if (!"USER".equals(applicant.getRole())) {
                throw new BusinessException("申请人当前身份已变化，无法授予骑手身份");
            }
            userService.grantRole(applicant.getPhone(), application.getTargetRole());
        }
        application.setStatus(status);
        application.setAdminRemarks(request.remarks());
        application.setOperatorId(adminId);
        roleApplicationMapper.updateById(application);
        notificationService.send(application.getUserId(),
            APPROVED.equals(status) ? "骑手申请已通过" : "骑手申请未通过",
            APPROVED.equals(status)
                ? "您的骑手身份申请已通过，请重新登录后进入骑手工作台。"
                : "您的骑手身份申请未通过" + (request.remarks() == null || request.remarks().isBlank() ? "。" : "：" + request.remarks()));
        return application;
    }

    private User requireUser(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }
}
