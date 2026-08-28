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

    /**
     * 交流封禁适用于所有用户主动发送的内容：评价、回复、投票和订单/咨询聊天。
     * 读取历史记录不受影响，避免影响已存在订单的履约追溯。
     */
    public void assertCanCommunicate(String userId) {
        assertCanComment(userId);
    }

    public void assertCanUsePlatform(String userId) {
        if (userId == null) {
            return;
        }
        UserPenalty stop = findActivePenalty(userId, SERVICE_STOP);
        if (stop != null) {
            throw new BusinessException("您的账号已被永久停止服务，无法使用平台功能");
        }
    }

    public boolean isAccountOnlyRestricted(String userId) {
        if (userId == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return userPenaltyMapper.selectOne(new LambdaQueryWrapper<UserPenalty>()
            .eq(UserPenalty::getUserId, userId)
            .eq(UserPenalty::getPenaltyType, SERVICE_STOP)
            .eq(UserPenalty::getActive, true)
            .and(w -> w.isNull(UserPenalty::getEndTime).or().gt(UserPenalty::getEndTime, now))
            .last("LIMIT 1")) != null;
    }

    /**
     * 账户封禁同时写入 user.enabled。登录前调用此方法可让已到期的封禁自动恢复账号。
     */
    @Transactional
    public UserPenalty activeAccountBan(String userId) {
        expireOutdatedPenalties(userId);
        return selectActivePenalty(userId, BAN);
    }

    public UserPenalty findActivePenalty(String userId, String type) {
        expireOutdatedPenalties(userId);
        return selectActivePenalty(userId, type);
    }

    private UserPenalty selectActivePenalty(String userId, String type) {
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
        if (BAN.equals(type)) {
            user.setEnabled(false);
            userMapper.updateById(user);
        }
        notificationService.send(request.userId(), "账号处罚通知", "管理员已对您的账号执行 " + penaltyLabel(type) + "：" + request.reason());
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
        if (BAN.equals(penalty.getPenaltyType())) {
            restoreAccountIfNoActiveBan(penalty.getUserId());
        }
        notificationService.send(penalty.getUserId(), "处罚已撤销", "管理员已撤销相关处罚记录。");
    }

    @Transactional
    public void restoreAccount(String userId, String adminId) {
        List<UserPenalty> bans = userPenaltyMapper.selectList(new LambdaQueryWrapper<UserPenalty>()
            .eq(UserPenalty::getUserId, userId)
            .eq(UserPenalty::getPenaltyType, BAN)
            .eq(UserPenalty::getActive, true));
        for (UserPenalty ban : bans) {
            ban.setActive(false);
            userPenaltyMapper.updateById(ban);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setEnabled(true);
        userMapper.updateById(user);
        notificationService.send(userId, "账户封禁已恢复", "管理员已恢复你的账户访问权限。");
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
        boolean accountBanExpired = false;
        for (UserPenalty item : expired) {
            item.setActive(false);
            userPenaltyMapper.updateById(item);
            accountBanExpired = accountBanExpired || BAN.equals(item.getPenaltyType());
        }
        if (accountBanExpired) {
            restoreAccountIfNoActiveBan(userId);
        }
    }

    private void restoreAccountIfNoActiveBan(String userId) {
        Long activeBanCount = userPenaltyMapper.selectCount(new LambdaQueryWrapper<UserPenalty>()
            .eq(UserPenalty::getUserId, userId)
            .eq(UserPenalty::getPenaltyType, BAN)
            .eq(UserPenalty::getActive, true)
            .and(w -> w.isNull(UserPenalty::getEndTime).or().gt(UserPenalty::getEndTime, LocalDateTime.now())));
        if (activeBanCount == 0) {
            User user = userMapper.selectById(userId);
            if (user != null && Boolean.FALSE.equals(user.getEnabled())) {
                user.setEnabled(true);
                userMapper.updateById(user);
            }
        }
    }

    private String penaltyLabel(String type) {
        return switch (type) {
            case MUTE -> "交流封禁";
            case BAN -> "账户封禁";
            case SERVICE_STOP -> "仅保留账户信息";
            default -> type;
        };
    }
}
