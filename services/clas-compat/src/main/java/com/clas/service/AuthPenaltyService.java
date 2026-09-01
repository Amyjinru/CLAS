package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.entity.UserPenalty;
import com.clas.mapper.UserPenaltyMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class AuthPenaltyService {
    private final UserPenaltyMapper userPenaltyMapper;

    public AuthPenaltyService(UserPenaltyMapper userPenaltyMapper) {
        this.userPenaltyMapper = userPenaltyMapper;
    }

    public boolean isAccountOnlyRestricted(String userId) {
        if (userId == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return userPenaltyMapper.selectOne(new LambdaQueryWrapper<UserPenalty>()
            .eq(UserPenalty::getUserId, userId)
            .eq(UserPenalty::getPenaltyType, "SERVICE_STOP")
            .eq(UserPenalty::getActive, true)
            .and(w -> w.isNull(UserPenalty::getEndTime).or().gt(UserPenalty::getEndTime, now))
            .last("LIMIT 1")) != null;
    }
}
