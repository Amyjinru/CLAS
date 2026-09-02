package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.IamClient;
import com.clas.common.BusinessException;
import com.clas.entity.RiderProfile;
import com.clas.entity.RiderWithdrawal;
import com.clas.mapper.RiderProfileMapper;
import com.clas.mapper.RiderWithdrawalMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderWithdrawalService {
    private final RiderWithdrawalMapper withdrawals;
    private final RiderProfileMapper profiles;
    private final IamClient iamClient;
    private final NotificationBridge notifications;

    public RiderWithdrawalService(
        RiderWithdrawalMapper withdrawals,
        RiderProfileMapper profiles,
        IamClient iamClient,
        NotificationBridge notifications
    ) {
        this.withdrawals = withdrawals;
        this.profiles = profiles;
        this.iamClient = iamClient;
        this.notifications = notifications;
    }

    @Transactional
    public RiderWithdrawal apply(String rider, Long cardId, Integer amount) {
        if (!iamClient.ownsBankCard(cardId, rider)) {
            throw new BusinessException("银行卡不存在或无权使用");
        }
        RiderProfile p = profiles.selectByUserIdForUpdate(rider);
        if (p == null || (p.getWithdrawableBalance() == null ? 0 : p.getWithdrawableBalance()) < amount) {
            throw new BusinessException("可提现余额不足");
        }
        p.setWithdrawableBalance(p.getWithdrawableBalance() - amount);
        p.setFrozenBalance((p.getFrozenBalance() == null ? 0 : p.getFrozenBalance()) + amount);
        p.setUpdatedAt(LocalDateTime.now());
        profiles.updateById(p);
        RiderWithdrawal w = new RiderWithdrawal();
        w.setRiderId(rider);
        w.setBankCardId(cardId);
        w.setAmount(amount);
        w.setStatus("PENDING");
        w.setCreatedAt(LocalDateTime.now());
        withdrawals.insert(w);
        notifications.send(new NotificationBridge.NotificationTarget(
            rider, "提现申请已提交", "提现申请已进入审核队列。",
            "RIDER_WITHDRAWAL", "WITHDRAWAL", w.getId(), null, null, null, null, "/rider-workbench"
        ));
        return w;
    }

    public List<RiderWithdrawal> mine(String rider) {
        return withdrawals.selectList(new LambdaQueryWrapper<RiderWithdrawal>()
            .eq(RiderWithdrawal::getRiderId, rider)
            .orderByDesc(RiderWithdrawal::getCreatedAt));
    }

    public List<RiderWithdrawal> pending() {
        return withdrawals.selectList(new LambdaQueryWrapper<RiderWithdrawal>()
            .eq(RiderWithdrawal::getStatus, "PENDING"));
    }

    @Transactional
    public RiderWithdrawal audit(Long id, boolean approved, String reason, String admin) {
        RiderWithdrawal w = withdrawals.selectById(id);
        if (w == null || !"PENDING".equals(w.getStatus())) {
            throw new BusinessException("提现申请不可审核");
        }
        RiderProfile p = profiles.selectByUserIdForUpdate(w.getRiderId());
        w.setStatus(approved ? "APPROVED" : "REJECTED");
        w.setReviewerId(admin);
        w.setReviewReason(reason.trim());
        w.setReviewedAt(LocalDateTime.now());
        if (!approved) {
            p.setFrozenBalance(Math.max(0, (p.getFrozenBalance() == null ? 0 : p.getFrozenBalance()) - w.getAmount()));
            p.setWithdrawableBalance((p.getWithdrawableBalance() == null ? 0 : p.getWithdrawableBalance()) + w.getAmount());
            p.setUpdatedAt(LocalDateTime.now());
            profiles.updateById(p);
        }
        withdrawals.updateById(w);
        notifications.send(new NotificationBridge.NotificationTarget(
            w.getRiderId(),
            approved ? "提现审核通过" : "提现审核未通过",
            approved ? "提现申请已审核通过。" : "提现申请未通过，金额已退回可提现余额。",
            "RIDER_WITHDRAWAL", "WITHDRAWAL", w.getId(), null, null, null, null, "/rider-workbench"
        ));
        return w;
    }
}
