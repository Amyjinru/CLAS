package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.RoleApplicationAuditRequest;
import com.clas.dto.RoleApplicationCreateRequest;
import com.clas.dto.RoleApplicationRecordResponse;
import com.clas.entity.Merchant;
import com.clas.entity.MerchantAuditLog;
import com.clas.entity.RoleApplication;
import com.clas.entity.RiderApplication;
import com.clas.entity.User;
import com.clas.mapper.MerchantAuditLogMapper;
import com.clas.mapper.MerchantMapper;
import com.clas.mapper.RoleApplicationMapper;
import com.clas.mapper.RiderApplicationMapper;
import com.clas.mapper.UserMapper;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final RiderApplicationMapper riderApplicationMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantAuditLogMapper merchantAuditLogMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final UserService userService;

    public RoleApplicationService(
        RoleApplicationMapper roleApplicationMapper,
        RiderApplicationMapper riderApplicationMapper,
        MerchantMapper merchantMapper,
        MerchantAuditLogMapper merchantAuditLogMapper,
        UserMapper userMapper,
        NotificationService notificationService,
        UserService userService
    ) {
        this.roleApplicationMapper = roleApplicationMapper;
        this.riderApplicationMapper = riderApplicationMapper;
        this.merchantMapper = merchantMapper;
        this.merchantAuditLogMapper = merchantAuditLogMapper;
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
        boolean merchantPending = merchantMapper.exists(new LambdaQueryWrapper<com.clas.entity.Merchant>()
            .eq(com.clas.entity.Merchant::getUserId, userId)
            .eq(com.clas.entity.Merchant::getStatus, com.clas.common.MerchantStatusEnum.PENDING));
        if (merchantPending) {
            throw new BusinessException("已有待审核的商家申请，暂不能申请骑手身份");
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

    /**
     * Returns all business-identity applications in one format so callers do
     * not need separate rider and merchant audit requests.
     */
    public List<RoleApplicationRecordResponse> listMineRecords(String userId) {
        List<RoleApplicationRecordResponse> records = new ArrayList<>();
        List<RiderApplication> riderApplications = riderApplicationMapper.selectList(new LambdaQueryWrapper<RiderApplication>()
            .eq(RiderApplication::getUserId, userId)
            .orderByDesc(RiderApplication::getCreatedAt));
        for (RiderApplication application : riderApplications) {
            String reason = "配送工具：" + application.getVehicleType() + "；服务区域：" + application.getServiceArea();
            records.add(new RoleApplicationRecordResponse(
                "rider-profile-" + application.getId(),
                RIDER,
                application.getStatus(),
                reason,
                application.getRejectReason(),
                application.getReviewerId(),
                application.getCreatedAt(),
                application.getReviewedAt() == null ? application.getCreatedAt() : application.getReviewedAt()
            ));
        }
        // 兼容历史通用身份记录；新骑手申请只以 rider_application 为唯一来源。
        for (RoleApplication application : listMine(userId)) {
            if (RIDER.equals(application.getTargetRole()) && !riderApplications.isEmpty()) {
                continue;
            }
            records.add(new RoleApplicationRecordResponse(
                "rider-" + application.getId(),
                application.getTargetRole(),
                application.getStatus(),
                application.getReason(),
                application.getAdminRemarks(),
                application.getOperatorId(),
                application.getCreatedAt(),
                application.getUpdatedAt()
            ));
        }

        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getUserId, userId));
        if (merchant != null) {
            MerchantAuditLog latestAudit = merchantAuditLogMapper.selectOne(new LambdaQueryWrapper<MerchantAuditLog>()
                .eq(MerchantAuditLog::getMerchantId, merchant.getId())
                .orderByDesc(MerchantAuditLog::getId)
                .last("LIMIT 1"));
            records.add(new RoleApplicationRecordResponse(
                "merchant-" + merchant.getId(),
                "MERCHANT",
                merchantApplicationStatus(merchant.getStatus() == null ? null : merchant.getStatus().name()),
                merchant.getMerchantName() + " 的入驻申请",
                merchant.getAdminRemarks() != null ? merchant.getAdminRemarks() : latestAudit == null ? null : latestAudit.getRemarks(),
                latestAudit == null ? null : latestAudit.getAdminId(),
                merchant.getCreatedAt(),
                merchant.getUpdatedAt()
            ));
        }
        records.sort(Comparator.comparing(RoleApplicationRecordResponse::createdAt,
            Comparator.nullsLast(Comparator.reverseOrder())));
        return records;
    }

    private String merchantApplicationStatus(String status) {
        if ("OPEN".equals(status) || "CLOSED".equals(status) || "APPROVED".equals(status)) {
            return APPROVED;
        }
        if ("BLOCKED".equals(status) || "DISABLED".equals(status)) {
            return REJECTED;
        }
        return PENDING;
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
