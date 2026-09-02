package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.IamClient;
import com.clas.client.MerchantClient;
import com.clas.client.OrderClient;
import com.clas.common.BusinessException;
import com.clas.common.MerchantStatusEnum;
import com.clas.config.UserContext;
import com.clas.dto.RiderAdminUpdateRequest;
import com.clas.dto.RiderApplicationRequest;
import com.clas.dto.RiderApplicationResponse;
import com.clas.dto.RiderIdentityRevealResponse;
import com.clas.dto.RiderProfileResponse;
import com.clas.dto.RoleApplicationRecordResponse;
import com.clas.entity.Merchant;
import com.clas.entity.RiderApplication;
import com.clas.entity.RiderAuditLog;
import com.clas.entity.RiderProfile;
import com.clas.mapper.RiderApplicationMapper;
import com.clas.mapper.RiderAuditLogMapper;
import com.clas.mapper.RiderProfileMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderApplicationService {
    private final RiderApplicationMapper applications;
    private final RiderProfileMapper profiles;
    private final IamClient iamClient;
    private final RiderIdentityCrypto crypto;
    private final RiderAuditLogMapper audits;
    private final NotificationBridge notifications;
    private final OrderClient orderClient;
    private final MerchantClient merchantClient;

    public RiderApplicationService(
        RiderApplicationMapper applications,
        RiderProfileMapper profiles,
        IamClient iamClient,
        RiderIdentityCrypto crypto,
        RiderAuditLogMapper audits,
        NotificationBridge notifications,
        OrderClient orderClient,
        MerchantClient merchantClient
    ) {
        this.applications = applications;
        this.profiles = profiles;
        this.iamClient = iamClient;
        this.crypto = crypto;
        this.audits = audits;
        this.notifications = notifications;
        this.orderClient = orderClient;
        this.merchantClient = merchantClient;
    }

    @Transactional
    public RiderApplicationResponse apply(RiderApplicationRequest request) {
        String userId = UserContext.getUserId();
        Long existingMerchantId = merchantClient.getMerchantIdByUser(userId);
        if (existingMerchantId != null) {
            Merchant merchant = merchantClient.getMerchant(existingMerchantId);
            if (merchant != null && merchant.getStatus() != MerchantStatusEnum.DISABLED) {
                throw new BusinessException("已有商家业务身份或入驻申请，暂不能申请骑手身份");
            }
        }
        RiderApplication latest = applications.selectOne(new LambdaQueryWrapper<RiderApplication>()
            .eq(RiderApplication::getUserId, userId)
            .orderByDesc(RiderApplication::getId)
            .last("LIMIT 1"));
        if (latest != null && "PENDING".equals(latest.getStatus())) {
            throw new BusinessException("已有待审核骑手申请");
        }
        boolean alreadyApproved = profiles.selectById(userId) != null;
        RiderApplication application = new RiderApplication();
        application.setUserId(userId);
        application.setRealName(request.realName().trim());
        application.setIdCardCiphertext(crypto.encrypt(request.idCardNo()));
        application.setIdCardMasked(crypto.mask(request.idCardNo()));
        application.setVehicleType(request.vehicleType().trim());
        application.setServiceArea(request.serviceArea().trim());
        application.setEmergencyContactName(request.emergencyContactName().trim());
        application.setEmergencyContactPhone(request.emergencyContactPhone());
        application.setCredentialUrls(request.credentialUrls());
        application.setStatus("PENDING");
        application.setCreatedAt(LocalDateTime.now());
        applications.insert(application);
        if (!alreadyApproved) {
            iamClient.upsertRoleStatus(userId, "RIDER", "PENDING");
        }
        return response(application);
    }

    public RiderApplicationResponse mine() {
        RiderApplication application = applications.selectOne(new LambdaQueryWrapper<RiderApplication>()
            .eq(RiderApplication::getUserId, UserContext.getUserId())
            .orderByDesc(RiderApplication::getId)
            .last("LIMIT 1"));
        if (application == null) {
            throw new BusinessException("尚未提交骑手申请");
        }
        return response(application);
    }

    public RiderProfileResponse profile() {
        RiderProfile profile = profiles.selectById(UserContext.getUserId());
        if (profile == null) {
            throw new BusinessException("骑手资料不存在或尚未审核通过");
        }
        return RiderProfileResponse.from(profile);
    }

    public java.util.List<RiderApplicationResponse> pending() {
        return applications.selectList(new LambdaQueryWrapper<RiderApplication>()
                .eq(RiderApplication::getStatus, "PENDING")
                .orderByAsc(RiderApplication::getCreatedAt))
            .stream()
            .map(this::response)
            .toList();
    }

    public RiderProfileResponse adminProfile(String riderId) {
        RiderProfile profile = profiles.selectById(riderId);
        if (profile == null) {
            throw new BusinessException("骑手档案不存在");
        }
        return RiderProfileResponse.from(profile);
    }

    @Transactional
    public RiderProfileResponse adminUpdate(String riderId, RiderAdminUpdateRequest request, String adminId) {
        RiderProfile profile = profiles.selectById(riderId);
        if (profile == null) {
            throw new BusinessException("骑手档案不存在");
        }
        if (!request.enabled()) {
            long active = orderClient.countActiveOrders(riderId, "ASSIGNED_WAITING_MEAL,DELIVERING");
            if (active > 0) {
                throw new BusinessException("骑手有进行中配送，不能直接停用", "DELIVERY_STATE_INVALID");
            }
        }
        profile.setStatus(request.enabled() ? "APPROVED" : "DISABLED");
        profile.setOnlineStatus(request.enabled() ? profile.getOnlineStatus() : false);
        profile.setAcceptingOrders(false);
        if (request.maxActiveOrders() != null) {
            profile.setMaxActiveOrders(request.maxActiveOrders());
        }
        profile.setUpdatedAt(LocalDateTime.now());
        profiles.updateById(profile);
        iamClient.upsertRoleStatus(riderId, "RIDER", request.enabled() ? "APPROVED" : "DISABLED");
        audit(riderId, adminId, request.enabled() ? "ENABLED" : "DISABLED", request.reason(), null, String.valueOf(request.maxActiveOrders()));
        return RiderProfileResponse.from(profile);
    }

    @Transactional
    public RiderIdentityRevealResponse revealIdentity(String riderId, String purpose, String adminId) {
        if (purpose == null || purpose.isBlank()) {
            throw new BusinessException("查看完整身份证号必须说明用途");
        }
        RiderProfile profile = profiles.selectById(riderId);
        if (profile == null) {
            throw new BusinessException("骑手档案不存在");
        }
        audit(riderId, adminId, "IDENTITY_REVEALED", purpose.trim(), null, null);
        return new RiderIdentityRevealResponse(riderId, profile.getRealName(), crypto.decrypt(profile.getIdCardCiphertext()), purpose.trim());
    }

    @Transactional
    public RiderApplicationResponse audit(Long id, String decision, String reason, Integer maxActiveOrders, String adminId) {
        RiderApplication application = applications.selectById(id);
        if (application == null || !"PENDING".equals(application.getStatus())) {
            throw new BusinessException("骑手申请不存在或已处理");
        }
        boolean approved = "APPROVE".equalsIgnoreCase(decision);
        boolean rejected = "REJECT".equalsIgnoreCase(decision);
        if (!approved && !rejected) {
            throw new BusinessException("审核结果只能是 APPROVE 或 REJECT");
        }
        if (rejected && (reason == null || reason.isBlank())) {
            throw new BusinessException("驳回原因不能为空");
        }
        application.setStatus(approved ? "APPROVED" : "REJECTED");
        application.setRejectReason(rejected ? reason.trim() : null);
        application.setReviewerId(adminId);
        application.setReviewedAt(LocalDateTime.now());
        applications.updateById(application);
        iamClient.upsertRoleStatus(application.getUserId(), "RIDER", application.getStatus());
        if (approved) {
            RiderProfile profile = profiles.selectById(application.getUserId());
            if (profile == null) {
                profile = new RiderProfile();
                profile.setUserId(application.getUserId());
                profile.setCreatedAt(LocalDateTime.now());
                profile.setOnlineStatus(false);
                profile.setAcceptingOrders(false);
                profile.setWithdrawableBalance(0);
                profile.setFrozenBalance(0);
            }
            profile.setRealName(application.getRealName());
            profile.setIdCardCiphertext(application.getIdCardCiphertext());
            profile.setIdCardMasked(application.getIdCardMasked());
            profile.setVehicleType(application.getVehicleType());
            profile.setServiceArea(application.getServiceArea());
            profile.setEmergencyContactName(application.getEmergencyContactName());
            profile.setEmergencyContactPhone(application.getEmergencyContactPhone());
            profile.setStatus("APPROVED");
            profile.setMaxActiveOrders(maxActiveOrders == null ? 3 : Math.max(1, Math.min(10, maxActiveOrders)));
            profile.setUpdatedAt(LocalDateTime.now());
            if (profile.getServicePhone() == null || profile.getServicePhone().isBlank()) {
                profile.setServicePhone(application.getUserId());
            }
            if (profiles.selectById(profile.getUserId()) == null) {
                profiles.insert(profile);
            } else {
                profiles.updateById(profile);
            }
        }
        RiderAuditLog log = new RiderAuditLog();
        log.setRiderId(application.getUserId());
        log.setOperatorId(adminId);
        log.setAction(application.getStatus());
        log.setReason(reason);
        log.setCreatedAt(LocalDateTime.now());
        audits.insert(log);
        notifications.send(new NotificationBridge.NotificationTarget(
            application.getUserId(),
            approved ? "骑手申请已通过" : "骑手申请未通过",
            approved ? "你的骑手身份已开通，可切换身份后开始接单。" : "骑手申请未通过：" + reason,
            "RIDER_APPLICATION",
            "RIDER",
            application.getId(),
            null,
            null,
            null,
            null,
            "/rider"
        ));
        return response(application);
    }

    public java.util.List<RoleApplicationRecordResponse> listApplicationRecords(String userId) {
        return applications.selectList(new LambdaQueryWrapper<RiderApplication>()
                .eq(RiderApplication::getUserId, userId)
                .orderByDesc(RiderApplication::getCreatedAt))
            .stream()
            .map(item -> new RoleApplicationRecordResponse(
                String.valueOf(item.getId()),
                "RIDER",
                item.getStatus(),
                item.getRejectReason(),
                null,
                item.getReviewerId(),
                item.getCreatedAt(),
                item.getReviewedAt()
            ))
            .toList();
    }

    private void audit(String riderId, String operatorId, String action, String reason, String before, String after) {
        RiderAuditLog item = new RiderAuditLog();
        item.setRiderId(riderId);
        item.setOperatorId(operatorId);
        item.setAction(action);
        item.setReason(reason);
        item.setBeforeValue(before);
        item.setAfterValue(after);
        item.setCreatedAt(LocalDateTime.now());
        audits.insert(item);
    }

    private RiderApplicationResponse response(RiderApplication item) {
        return new RiderApplicationResponse(
            item.getId(),
            item.getRealName(),
            item.getIdCardMasked(),
            item.getVehicleType(),
            item.getServiceArea(),
            item.getStatus(),
            item.getRejectReason(),
            item.getReviewedAt(),
            item.getCreatedAt()
        );
    }
}
