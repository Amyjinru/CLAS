package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.clas.mapper.GroupDealMapper;
import com.clas.mapper.MerchantMapper;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DealService {
    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_UNUSED = "UNUSED";
    public static final String STATUS_USED = "USED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REFUNDED = "REFUNDED";

    private final GroupDealMapper groupDealMapper;
    private final DealOrderMapper dealOrderMapper;
    private final DealRedeemLogMapper dealRedeemLogMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantService merchantService;
    private final NotificationService notificationService;
    private final PenaltyService penaltyService;

    public DealService(
        GroupDealMapper groupDealMapper,
        DealOrderMapper dealOrderMapper,
        DealRedeemLogMapper dealRedeemLogMapper,
        MerchantMapper merchantMapper,
        MerchantService merchantService,
        NotificationService notificationService,
        PenaltyService penaltyService
    ) {
        this.groupDealMapper = groupDealMapper;
        this.dealOrderMapper = dealOrderMapper;
        this.dealRedeemLogMapper = dealRedeemLogMapper;
        this.merchantMapper = merchantMapper;
        this.merchantService = merchantService;
        this.notificationService = notificationService;
        this.penaltyService = penaltyService;
    }

    public List<GroupDeal> list(Long merchantId) {
        LambdaQueryWrapper<GroupDeal> wrapper = new LambdaQueryWrapper<GroupDeal>()
            .eq(GroupDeal::getStatus, "ON_SALE")
            .orderByDesc(GroupDeal::getId);
        if (merchantId != null) {
            wrapper.eq(GroupDeal::getMerchantId, merchantId);
        }
        return groupDealMapper.selectList(wrapper);
    }

    public GroupDeal getById(Long dealId) {
        GroupDeal deal = groupDealMapper.selectById(dealId);
        if (deal == null) {
            throw new BusinessException("团购券不存在");
        }
        return deal;
    }

    public List<GroupDeal> merchantDeals() {
        return groupDealMapper.selectList(new LambdaQueryWrapper<GroupDeal>()
            .eq(GroupDeal::getMerchantId, merchantService.getCurrentMerchantId())
            .orderByDesc(GroupDeal::getId));
    }

    public GroupDeal create(com.clas.dto.DealRequest request) {
        String status = request.status() == null || request.status().isBlank() ? "ON_SALE" : request.status();
        if (!"ON_SALE".equals(status) && !"OFF_SALE".equals(status)) {
            throw new BusinessException("团购状态只能是 ON_SALE 或 OFF_SALE");
        }
        GroupDeal deal = new GroupDeal();
        deal.setMerchantId(merchantService.getCurrentMerchantId());
        deal.setTitle(request.title());
        deal.setDescription(request.description());
        deal.setOriginalPrice(request.originalPrice());
        deal.setDealPrice(request.dealPrice());
        deal.setStock(request.stock());
        deal.setValidDays(request.validDays());
        deal.setStatus(status);
        groupDealMapper.insert(deal);
        return deal;
    }

    @Transactional
    public DealOrder buy(Long dealId) {
        penaltyService.assertCanUsePlatform(UserContext.getUserId());
        GroupDeal deal = groupDealMapper.selectById(dealId);
        if (deal == null || !"ON_SALE".equals(deal.getStatus())) {
            throw new BusinessException("团购券不存在或已下架");
        }
        assertMerchantOpenNow(deal.getMerchantId());
        if (deal.getStock() <= 0) {
            throw new BusinessException("团购券库存不足");
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
            notificationService.send(order.getUserId(), "团购券待支付", "您有一笔团购订单待支付，请尽快完成支付。");
        } catch (RuntimeException ignored) {
            // 通知失败不应影响下单与跳转支付。
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
            throw new BusinessException("团购订单当前不可支付，状态：" + order.getStatus());
        }
        GroupDeal deal = groupDealMapper.selectById(order.getDealId());
        if (deal == null || !"ON_SALE".equals(deal.getStatus())) {
            throw new BusinessException("团购券不存在或已下架");
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
            Thread.sleep(100); // 模拟支付延迟（生产环境应异步处理）
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("模拟支付被中断");
        }

        int rows = groupDealMapper.deductStock(deal.getId());
        if (rows == 0) {
            throw new BusinessException("团购券库存不足");
        }

        LocalDateTime paidTime = LocalDateTime.now();
        order.setStatus(STATUS_UNUSED);
        order.setVoucherCode("CLAS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setPaidTime(paidTime);
        order.setExpireTime(paidTime.plusDays(deal.getValidDays() == null ? 30 : deal.getValidDays()));
        dealOrderMapper.updateById(order);
        try {
            notificationService.send(
                order.getUserId(),
                "团购券购买成功",
                "券码 " + order.getVoucherCode() + " 已生成，有效期至 "
                    + order.getExpireTime().toLocalDate() + "，可到店核销。"
            );
        } catch (RuntimeException ignored) {
            // 通知失败不应影响支付结果。
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
            throw new BusinessException("团购订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该团购订单");
        }
        return order;
    }

    public List<DealOrder> myOrders() {
        List<DealOrder> orders = dealOrderMapper.selectList(new LambdaQueryWrapper<DealOrder>()
            .eq(DealOrder::getUserId, UserContext.getUserId())
            .orderByDesc(DealOrder::getId));
        orders.forEach(this::refreshExpiredStatus);
        return orders;
    }

    @Transactional
    public DealOrder redeem(String voucherCode) {
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new BusinessException("券码不能为空");
        }
        DealOrder order = dealOrderMapper.selectOne(new LambdaQueryWrapper<DealOrder>()
            .eq(DealOrder::getVoucherCode, voucherCode.trim()));
        if (order == null) {
            throw new BusinessException("券码不存在");
        }
        if (!merchantService.getCurrentMerchantId().equals(order.getMerchantId())) {
            throw new BusinessException("只能核销自己店铺的团购券");
        }
        refreshExpiredStatus(order);
        if (STATUS_EXPIRED.equals(order.getStatus())) {
            throw new BusinessException("该团购券已过期");
        }
        if (!STATUS_UNUSED.equals(order.getStatus())) {
            throw new BusinessException("该团购券已核销或已失效");
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

        notificationService.send(order.getUserId(), "团购券已核销", "券码 " + order.getVoucherCode() + " 已成功核销。");
        return order;
    }

    @Transactional
    public DealOrder refundDealOrder(Long dealOrderId, String userId) {
        DealOrder order = requireUserDealOrder(dealOrderId, userId);
        refreshExpiredStatus(order);
        if (!STATUS_UNUSED.equals(order.getStatus())) {
            throw new BusinessException("仅未使用的团购券可申请退款");
        }
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("团购券已过期，无法退款");
        }
        groupDealMapper.restoreStock(order.getDealId());
        order.setStatus(STATUS_REFUNDED);
        dealOrderMapper.updateById(order);
        notificationService.send(userId, "团购券已退款", "券码 " + order.getVoucherCode() + " 已退款。");
        return order;
    }

    public List<DealRedeemLogResponse> redeemLogs() {
        Long merchantId = merchantService.getCurrentMerchantId();
        List<DealRedeemLog> logs = dealRedeemLogMapper.selectList(new LambdaQueryWrapper<DealRedeemLog>()
            .eq(DealRedeemLog::getMerchantId, merchantId)
            .orderByDesc(DealRedeemLog::getId));
        return logs.stream().map(DealRedeemLogResponse::from).toList();
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
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BusinessException("商家不存在");
        }
        if (Boolean.TRUE.equals(merchant.getManualClosed())) {
            throw new BusinessException("商家已打烊");
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
                throw new BusinessException("商家已休息，当前营业时间：" + businessHours);
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
