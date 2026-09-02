package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.IamClient;
import com.clas.common.BusinessException;
import com.clas.common.PhoneValidator;
import com.clas.config.UserContext;
import com.clas.dto.InternalUserProfile;
import com.clas.dto.RiderInfoResponse;
import com.clas.dto.RiderInfoUpdateRequest;
import com.clas.dto.RiderPhoneChangeAuditRequest;
import com.clas.dto.RiderPhoneChangeRequest;
import com.clas.dto.RiderPhoneChangeResponse;
import com.clas.entity.RiderProfile;
import com.clas.entity.RiderProfileChangeRequest;
import com.clas.entity.RiderReview;
import com.clas.entity.User;
import com.clas.mapper.RiderProfileChangeRequestMapper;
import com.clas.mapper.RiderProfileMapper;
import com.clas.mapper.RiderReviewMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderInfoService {
    private final RiderProfileMapper profiles;
    private final RiderReviewMapper reviews;
    private final RiderProfileChangeRequestMapper changeRequests;
    private final IamClient iamClient;
    private final UserProfileService userProfileService;
    private final NotificationBridge notifications;

    public RiderInfoService(RiderProfileMapper profiles, RiderReviewMapper reviews,
                            RiderProfileChangeRequestMapper changeRequests, IamClient iamClient,
                            UserProfileService userProfileService, NotificationBridge notifications) {
        this.profiles = profiles;
        this.reviews = reviews;
        this.changeRequests = changeRequests;
        this.iamClient = iamClient;
        this.userProfileService = userProfileService;
        this.notifications = notifications;
    }

    @Transactional
    public RiderInfoResponse mine() {
        String riderId = UserContext.getUserId();
        return response(ensureProfile(riderId), requireUser(riderId), latestChange(riderId));
    }

    @Transactional
    public RiderInfoResponse updateMine(RiderInfoUpdateRequest request) {
        String riderId = UserContext.getUserId();
        RiderProfile profile = ensureProfile(riderId);
        profile.setVehicleType(request.vehicleType().trim());
        profile.setServiceArea(request.serviceArea().trim());
        profile.setEmergencyContactName(request.emergencyContactName().trim());
        profile.setEmergencyContactPhone(PhoneValidator.normalizeAndValidate(request.emergencyContactPhone()));
        profile.setUpdatedAt(LocalDateTime.now());
        profiles.updateById(profile);
        return response(profile, requireUser(riderId), latestChange(riderId));
    }

    @Transactional
    public RiderPhoneChangeResponse requestServicePhoneChange(RiderPhoneChangeRequest request) {
        String riderId = UserContext.getUserId();
        RiderProfile profile = ensureProfile(riderId);
        String requestedPhone = PhoneValidator.normalizeAndValidate(request.phone());
        String currentPhone = servicePhone(profile, requireUser(riderId));
        if (requestedPhone.equals(currentPhone)) {
            throw new BusinessException("新服务联系电话不能与当前号码相同");
        }
        RiderProfileChangeRequest pending = changeRequests.selectOne(new LambdaQueryWrapper<RiderProfileChangeRequest>()
            .eq(RiderProfileChangeRequest::getRiderId, riderId)
            .eq(RiderProfileChangeRequest::getStatus, "PENDING")
            .last("LIMIT 1"));
        if (pending != null) {
            throw new BusinessException("已有待审核的服务联系电话修改申请");
        }
        LocalDateTime now = LocalDateTime.now();
        RiderProfileChangeRequest change = new RiderProfileChangeRequest();
        change.setRiderId(riderId);
        change.setCurrentPhone(currentPhone);
        change.setRequestedPhone(requestedPhone);
        change.setStatus("PENDING");
        change.setCreatedAt(now);
        change.setUpdatedAt(now);
        changeRequests.insert(change);
        notifications.notifyAdmins("骑手资料修改待审核", "骑手 " + riderId + " 申请修改服务联系电话。");
        return RiderPhoneChangeResponse.from(change);
    }

    public List<RiderPhoneChangeResponse> pendingChanges() {
        return changeRequests.selectList(new LambdaQueryWrapper<RiderProfileChangeRequest>()
                .eq(RiderProfileChangeRequest::getStatus, "PENDING")
                .orderByAsc(RiderProfileChangeRequest::getCreatedAt))
            .stream().map(RiderPhoneChangeResponse::from).toList();
    }

    @Transactional
    public RiderPhoneChangeResponse auditChange(Long id, RiderPhoneChangeAuditRequest request, String adminId) {
        RiderProfileChangeRequest change = changeRequests.selectById(id);
        if (change == null || !"PENDING".equals(change.getStatus())) {
            throw new BusinessException("该资料修改申请不存在或已处理");
        }
        boolean approved = Boolean.TRUE.equals(request.approved());
        String reason = request.reason() == null ? null : request.reason().trim();
        if (!approved && (reason == null || reason.isBlank())) {
            throw new BusinessException("驳回时请填写审核说明");
        }
        LocalDateTime now = LocalDateTime.now();
        change.setStatus(approved ? "APPROVED" : "REJECTED");
        change.setReviewReason(reason == null || reason.isBlank() ? null : reason);
        change.setReviewerId(adminId);
        change.setReviewedAt(now);
        change.setUpdatedAt(now);
        if (approved) {
            RiderProfile profile = ensureProfile(change.getRiderId());
            profile.setServicePhone(change.getRequestedPhone());
            profile.setUpdatedAt(now);
            profiles.updateById(profile);
        }
        changeRequests.updateById(change);
        notifications.send(change.getRiderId(), approved ? "骑手服务联系电话已更新" : "骑手服务联系电话修改未通过",
            approved ? "你的服务联系电话已通过审核并生效。" : "本次修改未通过：" + change.getReviewReason());
        return RiderPhoneChangeResponse.from(change);
    }

    private RiderInfoResponse response(RiderProfile profile, User user, RiderProfileChangeRequest latestChange) {
        List<RiderReview> riderReviews = reviews.selectList(new LambdaQueryWrapper<RiderReview>()
            .eq(RiderReview::getRiderId, profile.getUserId()));
        long count = riderReviews.size();
        double rating = count == 0 ? 0D : riderReviews.stream()
            .map(RiderReview::getScore).filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue).average().orElse(0D);
        return new RiderInfoResponse(
            profile.getUserId(), userProfileService.displayName(user), userProfileService.avatarOf(user),
            profile.getRealName(), profile.getIdCardMasked(), profile.getVehicleType(), profile.getServiceArea(),
            profile.getEmergencyContactName(), profile.getEmergencyContactPhone(), servicePhone(profile, user),
            profile.getStatus(), profile.getOnlineStatus(), profile.getAcceptingOrders(), profile.getMaxActiveOrders(),
            Math.round(rating * 10D) / 10D, count,
            latestChange == null ? null : RiderPhoneChangeResponse.from(latestChange)
        );
    }

    private RiderProfile ensureProfile(String riderId) {
        RiderProfile profile = profiles.selectById(riderId);
        if (profile != null) return profile;
        User user = requireUser(riderId);
        LocalDateTime now = LocalDateTime.now();
        profile = new RiderProfile();
        profile.setUserId(riderId);
        profile.setRealName(userProfileService.displayName(user));
        profile.setIdCardCiphertext("PENDING_COMPLETION");
        profile.setIdCardMasked("待补充");
        profile.setVehicleType("待补充");
        profile.setServiceArea("待设置");
        profile.setEmergencyContactName("待补充");
        profile.setEmergencyContactPhone(user.getPhone());
        profile.setServicePhone(user.getPhone());
        profile.setOnlineStatus(false);
        profile.setAcceptingOrders(false);
        profile.setMaxActiveOrders(3);
        profile.setWithdrawableBalance(0);
        profile.setFrozenBalance(0);
        profile.setStatus("APPROVED");
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        profiles.insert(profile);
        return profile;
    }

    private User requireUser(String riderId) {
        InternalUserProfile profile = iamClient.getUserProfile(riderId);
        if (profile == null) {
            throw new BusinessException("用户不存在");
        }
        User user = new User();
        user.setPhone(profile.phone());
        user.setUsername(profile.username());
        user.setRole(profile.role());
        user.setEnabled(profile.enabled());
        user.setNickname(profile.nickname());
        user.setAvatar(profile.avatar());
        return user;
    }

    private RiderProfileChangeRequest latestChange(String riderId) {
        return changeRequests.selectOne(new LambdaQueryWrapper<RiderProfileChangeRequest>()
            .eq(RiderProfileChangeRequest::getRiderId, riderId)
            .orderByDesc(RiderProfileChangeRequest::getId)
            .last("LIMIT 1"));
    }

    private String servicePhone(RiderProfile profile, User user) {
        return profile.getServicePhone() == null || profile.getServicePhone().isBlank()
            ? user.getPhone() : profile.getServicePhone();
    }
}
