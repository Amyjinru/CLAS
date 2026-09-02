package com.clas.service;

import com.clas.client.OrderClient;
import com.clas.entity.Orders;
import com.clas.entity.RiderProfile;
import com.clas.entity.RiderSettlement;
import com.clas.mapper.RiderProfileMapper;
import com.clas.mapper.RiderSettlementMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RiderSettlementService {
    private static final String COMMISSION = "DELIVERY_COMMISSION";
    private final RiderSettlementMapper settlements;
    private final RiderProfileMapper profiles;
    private final OrderClient orderClient;
    public RiderSettlementService(RiderSettlementMapper settlements, RiderProfileMapper profiles, OrderClient orderClient) { this.settlements = settlements; this.profiles = profiles; this.orderClient = orderClient; }

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
        pending.setBalanceType("SETTLED");
        settlements.updateById(pending);
        profile.setWithdrawableBalance((profile.getWithdrawableBalance() == null ? 0 : profile.getWithdrawableBalance()) + pending.getAmount());
        profile.setUpdatedAt(LocalDateTime.now()); profiles.updateById(profile);
    }

    /** A delivered order's commission remains temporary for the 15-minute refund window. */
    @Scheduled(fixedDelayString = "${app.rider-settlement.release-delay-ms:60000}")
    @Transactional
    public void releaseMaturedDeliveryCommissions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<Orders> delivered = orderClient.listMaturedDeliveries(cutoff);
        delivered.forEach(this::releaseCommissionIfEligible);
    }

    @Transactional
    public void releaseCommissionIfEligible(Orders order) {
        LocalDateTime deliveredAt = order.getDeliveryCompletedAt() == null ? order.getDeliveredAt() : order.getDeliveryCompletedAt();
        if (!"DELIVERED".equals(order.getDeliveryStatus()) || deliveredAt == null
            || deliveredAt.plusMinutes(15).isAfter(LocalDateTime.now())
            || !(OrderService.STATUS_ACCEPTED.equals(order.getStatus()) || OrderService.STATUS_COMPLETED.equals(order.getStatus()))
            || !(order.getRefundStatus() == null || "NONE".equals(order.getRefundStatus()) || "REJECTED".equals(order.getRefundStatus()))) return;
        makeCommissionWithdrawable(order);
    }

    /** Refund approvals discard temporary commission and reverse it if it was already released. */
    @Transactional
    public void reverseCommissionForRefund(Orders order) {
        if (order.getRiderId() == null) return;
        String orderId = String.valueOf(order.getId());
        List<RiderSettlement> pendingEntries = settlements.selectBySourceForUpdate(COMMISSION, orderId);
        pendingEntries.stream().filter(entry -> "PENDING".equals(entry.getBalanceType()) || "SETTLED".equals(entry.getBalanceType())).forEach(entry -> {
            entry.setBalanceType("REFUND_REVERSED");
            settlements.updateById(entry);
        });
        List<RiderSettlement> releasedEntries = settlements.selectBySourceForUpdate("DELIVERY_COMMISSION_RELEASE", orderId);
        int releasedAmount = releasedEntries.stream().filter(entry -> "WITHDRAWABLE".equals(entry.getBalanceType()))
            .mapToInt(entry -> entry.getAmount() == null ? 0 : entry.getAmount()).sum();
        if (releasedAmount <= 0) return;
        RiderProfile profile = profiles.selectByUserIdForUpdate(order.getRiderId());
        if (profile != null) {
            int balance = profile.getWithdrawableBalance() == null ? 0 : profile.getWithdrawableBalance();
            profile.setWithdrawableBalance(Math.max(0, balance - releasedAmount));
            profile.setUpdatedAt(LocalDateTime.now());
            profiles.updateById(profile);
        }
        releasedEntries.stream().filter(entry -> "WITHDRAWABLE".equals(entry.getBalanceType())).forEach(entry -> {
            entry.setBalanceType("REFUND_REVERSED");
            settlements.updateById(entry);
        });
    }

    private RiderSettlement entry(Orders order, int amount, String balanceType) {
        RiderSettlement entry = new RiderSettlement();
        entry.setRiderId(order.getRiderId()); entry.setOrderId(order.getId()); entry.setSourceType(COMMISSION);
        entry.setSourceId(String.valueOf(order.getId())); entry.setSettlementType("COMMISSION"); entry.setAmount(amount);
        entry.setBalanceType(balanceType); entry.setCreatedAt(LocalDateTime.now()); return entry;
    }
}
