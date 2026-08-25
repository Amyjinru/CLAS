package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.config.UserContext;
import com.clas.dto.RiderLocationRequest;
import com.clas.dto.RiderProfileResponse;
import com.clas.entity.RiderLocationHistory;
import com.clas.entity.RiderProfile;
import com.clas.mapper.RiderLocationHistoryMapper;
import com.clas.mapper.RiderProfileMapper;
import com.clas.mapper.OrdersMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiderLocationService {
    private final RiderProfileMapper profiles;
    private final RiderLocationHistoryMapper history;
    public RiderLocationService(RiderProfileMapper profiles, RiderLocationHistoryMapper history, OrdersMapper orders) { this.profiles = profiles; this.history = history; }

    @Transactional
    public RiderProfileResponse setOnline(boolean online) {
        RiderProfile profile = approvedProfileForUpdate();
        profile.setOnlineStatus(online); profile.setUpdatedAt(LocalDateTime.now()); profiles.updateById(profile);
        return RiderProfileResponse.from(profile);
    }

    @Transactional
    public RiderProfileResponse setAcceptingOrders(boolean accepting) {
        RiderProfile profile = approvedProfileForUpdate();
        // “开始接单”是骑手明确开始工作的动作，必须同时进入在线状态。
        if (accepting) profile.setOnlineStatus(true);
        profile.setAcceptingOrders(accepting);
        profile.setUpdatedAt(LocalDateTime.now());
        profiles.updateById(profile);
        return RiderProfileResponse.from(profile);
    }

    @Transactional
    public RiderProfileResponse reportLocation(RiderLocationRequest request) {
        RiderProfile profile = approvedProfile();
        if (!Boolean.TRUE.equals(profile.getOnlineStatus())) throw new BusinessException("骑手需上线后才能上报位置");
        LocalDateTime now = LocalDateTime.now();
        profile.setCurrentLongitude(request.longitude()); profile.setCurrentLatitude(request.latitude());
        profile.setLocationUpdatedAt(now); profile.setUpdatedAt(now); profiles.updateById(profile);
        RiderLocationHistory item = new RiderLocationHistory();
        item.setRiderId(profile.getUserId()); item.setLongitude(request.longitude()); item.setLatitude(request.latitude());
        item.setAccuracyMeters(request.accuracyMeters()); item.setReportedAt(now); history.insert(item);
        return RiderProfileResponse.from(profile);
    }

    public RiderProfile approvedProfile() {
        RiderProfile profile = profiles.selectById(UserContext.getUserId());
        if (profile == null || !"APPROVED".equals(profile.getStatus())) throw new BusinessException(403, "骑手身份尚未审核通过或已被停用");
        return profile;
    }

    private RiderProfile approvedProfileForUpdate() {
        RiderProfile profile = profiles.selectByUserIdForUpdate(UserContext.getUserId());
        if (profile == null || !"APPROVED".equals(profile.getStatus())) throw new BusinessException(403, "骑手身份尚未审核通过或已被停用");
        return profile;
    }
}
