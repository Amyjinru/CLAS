package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.CatalogClient;
import com.clas.client.MerchantClient;
import com.clas.client.IamClient;
import com.clas.common.BusinessException;
import com.clas.config.UserContext;
import com.clas.dto.DealRedeemLogResponse;
import com.clas.dto.PaymentResponse;
import com.clas.entity.DealOrder;
import com.clas.entity.DealRedeemLog;
import com.clas.entity.GroupDeal;
import com.clas.entity.Merchant;
import com.clas.mapper.DealOrderMapper;
import com.clas.mapper.DealRedeemLogMapper;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DealOrderService {
    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_UNUSED = "UNUSED";
    public static final String STATUS_USED = "USED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REFUNDED = "REFUNDED";

    private final CatalogClient catalogClient;
    private final MerchantClient merchantClient;
    private final IamClient iamClient;
    private final MerchantContextService merchantContextService;
    private final NotificationBridge notificationBridge;
    private final DealOrderMapper dealOrderMapper;
    private final DealRedeemLogMapper dealRedeemLogMapper;

    public DealOrderService(
        CatalogClient catalogClient,
        MerchantClient merchantClient,
        IamClient iamClient,
        MerchantContextService merchantContextService,
        NotificationBridge notificationBridge,
        DealOrderMapper dealOrderMapper,
        DealRedeemLogMapper dealRedeemLogMapper
    ) {
        this.catalogClient = catalogClient;
        this.merchantClient = merchantClient;
        this.iamClient = iamClient;
        this.merchantContextService = merchantContextService;
        this.notificationBridge = notificationBridge;
        this.dealOrderMapper = dealOrderMapper;
        this.dealRedeemLogMapper = dealRedeemLogMapper;
    }

    @Transactional
    public DealOrder buy(Long dealId) {
        iamClient.assertCanUsePlatform(UserContext.getUserId());
        GroupDeal deal = catalogClient.getDeal(dealId);
        if (deal == null || !"ON_SALE".equals(deal.getStatus())) {
            throw new BusinessException("??????????");
        }
        assertMerchantOpenNow(deal.getMerchantId());
        if (deal.getStock() <= 0) {
            throw new BusinessException("???????");
        }

        DealOrder order = new DealOrder();
        order.setDealId(deal.getId());
        order.setUserId(UserContext.getUserId());
        order.setMerchantId(deal.getMerchantId());
        order.setVoucherCode("PEND-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        order.setStatus(STATUS_PENDING_PAYMENT);
        order.setPayAmount(deal.getDealPrice());
        order.setCreateTime(LocalDateTime.now());
        dealOrderMapper.insert(order);
        try {
            sendDealOrderNotification(order, "??????", "????????????????????");
        } catch (RuntimeException ignored) {
            // ????????????????
        }
        return order;
    }

    @Transactional
    public PaymentResponse payDealOrder(Long dealOrderId, String userId, String payMethod) {
        DealOrder order = requireUserDealOrder(dealOrderId, userId);
        if (!STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            if (STATUS_UNUSED.equals(order.getStatus()) || STATUS_USED.equals(order.getStatus())) {
                return getDealPaymentStatus(dealOrderId, userId);
            }
            throw new BusinessException("??????????????" + order.getStatus());
        }
        GroupDeal deal = catalogClient.getDeal(order.getDealId());
        if (deal == null || !"ON_SALE".equals(deal.getStatus())) {
            throw new BusinessException("??????????");
        }

        String method = payMethod == null || payMethod.isBlank() ? "MOCK" : payMethod;
        if ("FAIL_MOCK".equals(method)) {
            return new PaymentResponse(
                null,
                order.getId(),
                order.getPayAmount(),
                method,
                "FAILED",
                order.getStatus(),
                LocalDateTime.now(),
                null
            );
        }

        try {
            Thread.sleep(100); // ?????????????????
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("???????");
        }

        if (!catalogClient.deductDealStock(deal.getId())) {
            throw new BusinessException("???????");
        }

        LocalDateTime paidTime = LocalDateTime.now();
        order.setStatus(STATUS_UNUSED);
        order.setVoucherCode("CLAS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setPaidTime(paidTime);
        order.setExpireTime(paidTime.plusDays(deal.getValidDays() == null ? 30 : deal.getValidDays()));
        dealOrderMapper.updateById(order);
        try {
            sendDealOrderNotification(
                order,
                "???????",
                "?? " + order.getVoucherCode() + " ???????? "
                    + order.getExpireTime().toLocalDate() + "???????"
            );
        } catch (RuntimeException ignored) {
            // ?????????????
        }
        return new PaymentResponse(
            null,
            order.getId(),
            order.getPayAmount(),
            method,
            "SUCCESS",
            order.getStatus(),
            paidTime,
            null
        );
    }

    public PaymentResponse getDealPaymentStatus(Long dealOrderId, String userId) {
        DealOrder order = requireUserDealOrder(dealOrderId, userId);
        refreshExpiredStatus(order);
        String paymentStatus = STATUS_PENDING_PAYMENT.equals(order.getStatus())
            ? "PENDING"
            : (STATUS_UNUSED.equals(order.getStatus()) || STATUS_USED.equals(order.getStatus()) ? "SUCCESS" : "FAILED");
        return new PaymentResponse(
            null,
            order.getId(),
            order.getPayAmount(),
            null,
            paymentStatus,
            order.getStatus(),
            order.getPaidTime() == null ? order.getCreateTime() : order.getPaidTime(),
            null
        );
    }

    public DealOrder requireUserDealOrder(Long dealOrderId, String userId) {
        DealOrder order = dealOrderMapper.selectById(dealOrderId);
        if (order == null) {
            throw new BusinessException("???????");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("?????????");
        }
        return order;
    }

    public List<DealOrder> myOrders() {
        List<DealOrder> orders = dealOrderMapper.selectList(new LambdaQueryWrapper<DealOrder>()
            .eq(DealOrder::getUserId, UserContext.getUserId())
            .orderByDesc(DealOrder::getId));
        orders.forEach(this::refreshExpiredStatus);
        Set<Long> dealIds = orders.stream().map(DealOrder::getDealId).collect(Collectors.toSet());
        Map<Long, GroupDeal> dealsById = dealIds.isEmpty() ? Map.of() : catalogClient.getDeals(dealIds);
        Set<Long> merchantIds = orders.stream().map(DealOrder::getMerchantId).collect(Collectors.toSet());
        Map<Long, Merchant> merchantsById = merchantIds.isEmpty() ? Map.of() : merchantClient.getMerchants(merchantIds);
        orders.forEach(order -> {
            GroupDeal deal = dealsById.get(order.getDealId());
            Merchant merchant = merchantsById.get(order.getMerchantId());
            order.setDealTitle(deal == null ? null : deal.getTitle());
            order.setMerchantName(merchant == null ? null : merchant.getMerchantName());
            order.setMerchantLogo(merchant == null ? null : merchant.getLogo());
        });
        return orders;
    }

    @Transactional
    public DealOrder redeem(String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new BusinessException("??????");
        }
        DealOrder order = dealOrderMapper.selectOne(new LambdaQueryWrapper<DealOrder>()
            .eq(DealOrder::getVoucherCode, voucherCode.trim()));
        if (order == null) {
            throw new BusinessException("?????");
        }
        if (!merchantContextService.getCurrentMerchantId().equals(order.getMerchantId())) {
            throw new BusinessException("????????????");
        }
        refreshExpiredStatus(order);
        if (STATUS_EXPIRED.equals(order.getStatus())) {
            throw new BusinessException("???????");
        }
        if (!STATUS_UNUSED.equals(order.getStatus())) {
            throw new BusinessException("???????????");
        }
        order.setStatus(STATUS_USED);
        order.setUsedTime(LocalDateTime.now());
        dealOrderMapper.updateById(order);

        DealRedeemLog log = new DealRedeemLog();
        log.setDealOrderId(order.getId());
        log.setMerchantId(order.getMerchantId());
        log.setVoucherCode(order.getVoucherCode());
        log.setOperatorId(UserContext.getUserId());
        log.setRedeemedAt(order.getUsedTime());
        dealRedeemLogMapper.insert(log);

        sendDealOrderNotification(order, "??????", "?? " + order.getVoucherCode() + " ??????");
        return order;
    }

    @Transactional
    public DealOrder refundDealOrder(Long dealOrderId, String userId) {
        DealOrder order = requireUserDealOrder(dealOrderId, userId);
        refreshExpiredStatus(order);
        if (!STATUS_UNUSED.equals(order.getStatus())) {
            throw new BusinessException("?????????????");
        }
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("???????????");
        }
        catalogClient.restoreDealStock(order.getDealId());
        order.setStatus(STATUS_REFUNDED);
        dealOrderMapper.updateById(order);
        sendDealOrderNotification(order, "??????", "?? " + order.getVoucherCode() + " ????");
        return order;
    }

    public List<DealRedeemLogResponse> redeemLogs() {
        Long merchantId = merchantContextService.getCurrentMerchantId();
        List<DealRedeemLog> logs = dealRedeemLogMapper.selectList(new LambdaQueryWrapper<DealRedeemLog>()
            .eq(DealRedeemLog::getMerchantId, merchantId)
            .orderByDesc(DealRedeemLog::getId));
        return logs.stream().map(DealRedeemLogResponse::from).toList();
    }

    private void sendDealOrderNotification(DealOrder order, String title, String content) {
        notificationBridge.send(new NotificationBridge.NotificationTarget(
            order.getUserId(),
            title,
            content,
            "DEAL_ORDER_STATUS",
            "DEAL_ORDER",
            order.getId(),
            null,
            null,
            order.getId(),
            order.getMerchantId(),
            "/deal-order/" + order.getId()
        ));
    }

    private void refreshExpiredStatus(DealOrder order) {
        if (!STATUS_UNUSED.equals(order.getStatus())) {
            return;
        }
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            order.setStatus(STATUS_EXPIRED);
            dealOrderMapper.updateById(order);
        }
    }

    private void assertMerchantOpenNow(Long merchantId) {
        Merchant merchant = merchantClient.getMerchant(merchantId);
        if (merchant == null) {
            throw new BusinessException("?????");
        }
        if (Boolean.TRUE.equals(merchant.getManualClosed())) {
            throw new BusinessException("?????");
        }
        String businessHours = trimToNull(merchant.getBusinessHours());
        if (businessHours == null) {
            return;
        }
        String[] parts = businessHours.split("-");
        if (parts.length != 2) {
            return;
        }
        try {
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            LocalTime now = LocalTime.now();
            if (!isWithinBusinessHours(now, start, end)) {
                throw new BusinessException("?????????????" + businessHours);
            }
        } catch (DateTimeParseException exception) {
            return;
        }
    }

    private boolean isWithinBusinessHours(LocalTime now, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
