package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.entity.UserPenalty;
import com.clas.mapper.UserPenaltyMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class CommentPenaltyService {
    public static final String MUTE = "MUTE";
    public static final String SERVICE_STOP = "SERVICE_STOP";

    private final UserPenaltyMapper userPenaltyMapper;

    public CommentPenaltyService(UserPenaltyMapper userPenaltyMapper) {
        this.userPenaltyMapper = userPenaltyMapper;
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

    private UserPenalty findActivePenalty(String userId, String penaltyType) {
        LocalDateTime now = LocalDateTime.now();
        return userPenaltyMapper.selectOne(new LambdaQueryWrapper<UserPenalty>()
            .eq(UserPenalty::getUserId, userId)
            .eq(UserPenalty::getPenaltyType, penaltyType)
            .eq(UserPenalty::getActive, true)
            .and(w -> w.isNull(UserPenalty::getEndTime).or().gt(UserPenalty::getEndTime, now))
            .last("LIMIT 1"));
    }
}
