package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.dto.AppealRequest;
import com.clas.entity.Appeal;
import com.clas.entity.UserPenalty;
import com.clas.mapper.AppealMapper;
import com.clas.mapper.UserPenaltyMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppealService {
    private final AppealMapper appealMapper;
    private final UserPenaltyMapper userPenaltyMapper;
    private final PenaltyService penaltyService;
    private final NotificationService notificationService;

    public AppealService(
        AppealMapper appealMapper,
        UserPenaltyMapper userPenaltyMapper,
        PenaltyService penaltyService,
        NotificationService notificationService
    ) {
        this.appealMapper = appealMapper;
        this.userPenaltyMapper = userPenaltyMapper;
        this.penaltyService = penaltyService;
        this.notificationService = notificationService;
    }

    public Appeal submit(String userId, AppealRequest request) {
        if (request.penaltyId() != null) {
            UserPenalty penalty = userPenaltyMapper.selectById(request.penaltyId());
            if (penalty == null || !penalty.getUserId().equals(userId)) {
                throw new BusinessException("关联处罚记录不存在");
            }
        }
        Appeal appeal = new Appeal();
        appeal.setUserId(userId);
        appeal.setPenaltyId(request.penaltyId());
        appeal.setContent(request.content());
        appeal.setStatus("PENDING");
        appeal.setCreatedAt(LocalDateTime.now());
        appealMapper.insert(appeal);
        notificationService.notifyAdmins("收到新的申诉", "用户 " + userId + " 提交了处罚申诉，请及时处理。");
        return appeal;
    }

    public List<Appeal> listMine(String userId) {
        return appealMapper.selectList(new LambdaQueryWrapper<Appeal>()
            .eq(Appeal::getUserId, userId)
            .orderByDesc(Appeal::getId));
    }

    public List<Appeal> listPending() {
        return appealMapper.selectList(new LambdaQueryWrapper<Appeal>()
            .eq(Appeal::getStatus, "PENDING")
            .orderByDesc(Appeal::getId));
    }

    @Transactional
    public Appeal process(Long appealId, String status, String adminReply, String adminId) {
        Appeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }
        String nextStatus = status == null || status.isBlank() ? "APPROVED" : status.toUpperCase();
        if (!"APPROVED".equals(nextStatus) && !"REJECTED".equals(nextStatus)) {
            throw new BusinessException("申诉状态只能是 APPROVED 或 REJECTED");
        }
        appeal.setStatus(nextStatus);
        appeal.setAdminReply(adminReply);
        appeal.setAdminId(adminId);
        appeal.setProcessedAt(LocalDateTime.now());
        appealMapper.updateById(appeal);
        if ("APPROVED".equals(nextStatus) && appeal.getPenaltyId() != null) {
            penaltyService.revokePenalty(appeal.getPenaltyId(), adminId);
        }
        notificationService.send(appeal.getUserId(), "申诉处理结果", adminReply == null ? "您的申诉已处理。" : adminReply);
        return appeal;
    }
}
