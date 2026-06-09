package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.PenaltyRequest;
import com.clas.entity.User;
import com.clas.entity.UserPenalty;
import com.clas.mapper.UserMapper;
import com.clas.mapper.UserPenaltyMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PenaltyService {
    public static final String MUTE = "MUTE";
    public static final String BAN = "BAN";
    public static final String SERVICE_STOP = "SERVICE_STOP";

    private final UserPenaltyMapper userPenaltyMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public PenaltyService(
        UserPenaltyMapper userPenaltyMapper,
        UserMapper userMapper,
        NotificationService notificationService
    ) {
        this.userPenaltyMapper = userPenaltyMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    public void assertCanComment(String userId) {
        if (userId == null) {
            return;
        }
        UserPenalty penalty = findActivePenalty(userId, MUTE);
        if (penalty != null) {
            throw new BusinessException("您已被禁言，暂时无法发表评论或评价");
        }
        UserPenalty stop = findActivePenalty(userId, SERVICE_STOP);
        if (stop != null) {
            throw new BusinessException("您的账号已被永久停止服务，无法发表评论或评价");
        }
    }

    public void assertCanUsePlatform(String userId) {
        if (userId == null) {
            return;
        }
        UserPenalty stop = findActivePenalty(userId, SERVICE_STOP);
        if (stop != null) {
            throw new BusinessException("您的账号已被永久停止服务，无法使用平台功能");
        }
        UserPenalty ban = findActivePenalty(userId, BAN);
        if (ban != null) {
            throw new BusinessException("您的账号已被封禁，暂时无法使用订餐、预约等平台功能");
        }
    }

    public UserPenalty findActivePenalty(String userId, String type) {
        expireOutdatedPenalties(userId);
        LocalDateTime now = LocalDateTime.now();
        return userPenaltyMapper.selectOne(new LambdaQueryWrapper<UserPenalty>()
            .eq(UserPenalty::getUserId, userId)
            .eq(UserPenalty::getPenaltyType, type)
            .eq(UserPenalty::getActive, true)
            .and(w -> w.isNull(UserPenalty::getEndTime).or().gt(UserPenalty::getEndTime, now))
            .orderByDesc(UserPenalty::getId)
            .last("LIMIT 1"));
    }

    public List<UserPenalty> listPenaltiesForUser(String userId) {
        expireOutdatedPenalties(userId);
        return userPenaltyMapper.selectList(new LambdaQueryWrapper<UserPenalty>()
            .eq(UserPenalty::getUserId, userId)
            .orderByDesc(UserPenalty::getId));
    }

    public List<UserPenalty> listActivePenalties(String userId) {
        expireOutdatedPenalties(userId);
        LocalDateTime now = LocalDateTime.now();
        return userPenaltyMapper.selectList(new LambdaQueryWrapper<UserPenalty>()
            .eq(UserPenalty::getUserId, userId)
            .eq(UserPenalty::getActive, true)
            .and(w -> w.isNull(UserPenalty::getEndTime).or().gt(UserPenalty::getEndTime, now))
            .orderByDesc(UserPenalty::getId));
    }

    @Transactional
    public UserPenalty applyPenalty(PenaltyRequest request, String adminId) {
        User user = userMapper.selectById(request.userId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String type = request.penaltyType().trim().toUpperCase();
        if (!MUTE.equals(type) && !BAN.equals(type) && !SERVICE_STOP.equals(type)) {
            throw new BusinessException("处罚类型只能是 MUTE、BAN 或 SERVICE_STOP");
        }
        deactivatePenalties(request.userId(), type);
        UserPenalty penalty = new UserPenalty();
        penalty.setUserId(request.userId());
        penalty.setPenaltyType(type);
        penalty.setReason(request.reason());
        penalty.setStartTime(LocalDateTime.now());
        penalty.setAdminId(adminId);
        penalty.setActive(true);
        penalty.setCreatedAt(LocalDateTime.now());
        if (SERVICE_STOP.equals(type)) {
            penalty.setEndTime(null);
        } else {
            int hours = request.durationHours() == null || request.durationHours() <= 0 ? 24 : request.durationHours();
            penalty.setEndTime(LocalDateTime.now().plusHours(hours));
        }
        userPenaltyMapper.insert(penalty);
        notificationService.send(request.userId(), "账号处罚通知", "管理员已对您的账号执行 " + type + " 处罚：" + request.reason());
        return penalty;
    }

    @Transactional
    public void revokePenalty(Long penaltyId, String adminId) {
        UserPenalty penalty = userPenaltyMapper.selectById(penaltyId);
        if (penalty == null) {
            throw new BusinessException("处罚记录不存在");
        }
        penalty.setActive(false);
        userPenaltyMapper.updateById(penalty);
        notificationService.send(penalty.getUserId(), "处罚已撤销", "管理员已撤销相关处罚记录。");
    }

    private void deactivatePenalties(String userId, String type) {
        List<UserPenalty> existing = userPenaltyMapper.selectList(new LambdaQueryWrapper<UserPenalty>()
            .eq(UserPenalty::getUserId, userId)
            .eq(UserPenalty::getPenaltyType, type)
            .eq(UserPenalty::getActive, true));
        for (UserPenalty item : existing) {
            item.setActive(false);
            userPenaltyMapper.updateById(item);
        }
    }

    private void expireOutdatedPenalties(String userId) {
        LocalDateTime now = LocalDateTime.now();
        List<UserPenalty> expired = userPenaltyMapper.selectList(new LambdaQueryWrapper<UserPenalty>()
            .eq(UserPenalty::getUserId, userId)
            .eq(UserPenalty::getActive, true)
            .isNotNull(UserPenalty::getEndTime)
            .le(UserPenalty::getEndTime, now));
        for (UserPenalty item : expired) {
            item.setActive(false);
            userPenaltyMapper.updateById(item);
        }
    }
}
