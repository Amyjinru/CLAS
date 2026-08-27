package com.clas.service;

import com.clas.entity.Orders;
import com.clas.entity.RiderProfile;
import com.clas.entity.RiderSettlement;
import com.clas.mapper.RiderProfileMapper;
import com.clas.mapper.RiderSettlementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RiderSettlementService {
    private static final String COMMISSION = "DELIVERY_COMMISSION";
    private final RiderSettlementMapper settlements;
    private final RiderProfileMapper profiles;
    public RiderSettlementService(RiderSettlementMapper settlements, RiderProfileMapper profiles) { this.settlements = settlements; this.profiles = profiles; }

    @Transactional
    public void createPendingCommission(Orders order) {
        if (order.getRiderId() == null) return;
        String sourceId = String.valueOf(order.getId());
        if (!settlements.selectBySourceForUpdate(COMMISSION, sourceId).isEmpty()) return;
        int amount = order.getRiderCommission() == null || order.getRiderCommission() <= 0 ? Math.max(0, order.getDeliveryFee() == null ? 0 : order.getDeliveryFee()) : order.getRiderCommission();
        RiderSettlement entry = entry(order, amount, "PENDING");
        settlements.insert(entry);
    }

    @Transactional
    public void makeCommissionWithdrawable(Orders order) {
        if (order.getRiderId() == null) return;
        List<RiderSettlement> entries = settlements.selectBySourceForUpdate(COMMISSION, String.valueOf(order.getId()));
        if (entries.isEmpty()) { createPendingCommission(order); entries = settlements.selectBySourceForUpdate(COMMISSION, String.valueOf(order.getId())); }
        if (!settlements.selectBySourceForUpdate("DELIVERY_COMMISSION_RELEASE", String.valueOf(order.getId())).isEmpty()) return;
        RiderSettlement pending = entries.stream().filter(it -> "PENDING".equals(it.getBalanceType())).findFirst().orElse(null);
        if (pending == null || pending.getAmount() <= 0) return;
        RiderProfile profile = profiles.selectByUserIdForUpdate(order.getRiderId());
        if (profile == null) return;
        RiderSettlement release = entry(order, pending.getAmount(), "WITHDRAWABLE");
        release.setSourceType("DELIVERY_COMMISSION_RELEASE"); release.setSourceId(String.valueOf(order.getId()));
        settlements.insert(release);
        profile.setWithdrawableBalance((profile.getWithdrawableBalance() == null ? 0 : profile.getWithdrawableBalance()) + pending.getAmount());
        profile.setUpdatedAt(LocalDateTime.now()); profiles.updateById(profile);
    }

    private RiderSettlement entry(Orders order, int amount, String balanceType) {
        RiderSettlement entry = new RiderSettlement();
        entry.setRiderId(order.getRiderId()); entry.setOrderId(order.getId()); entry.setSourceType(COMMISSION);
        entry.setSourceId(String.valueOf(order.getId())); entry.setSettlementType("COMMISSION"); entry.setAmount(amount);
        entry.setBalanceType(balanceType); entry.setCreatedAt(LocalDateTime.now()); return entry;
    }
}
