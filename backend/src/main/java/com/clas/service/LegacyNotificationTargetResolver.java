package com.clas.service;

import com.clas.entity.Notification;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LegacyNotificationTargetResolver {
    private static final Pattern ORDER_CONTENT = Pattern.compile("订单\\s*(\\d+)");
    private static final Pattern VOUCHER_CONTENT = Pattern.compile("券码\\s*([A-Za-z0-9-]+)");

    private static final Set<String> ORDER_TITLES = Set.of(
        "订单已创建",
        "商家已接单",
        "订单配送中",
        "订单已完成",
        "商家已拒单",
        "退款申请已提交",
        "退款已通过",
        "退款被拒绝"
    );
    private static final Set<String> REVIEW_TITLES = Set.of("商家回复了评价", "评价收到新回复");
    private static final Set<String> DEAL_ORDER_TITLES = Set.of("团购券待支付", "团购券购买成功", "团购券已核销", "团购券已退款");

    private LegacyNotificationTargetResolver() {
    }

    static Optional<LegacyTarget> resolve(Notification notification) {
        String title = safe(notification.getTitle());
        String content = safe(notification.getContent());

        if (ORDER_TITLES.contains(title)) {
            return findOrderId(content)
                .map(orderId -> new LegacyTarget("ORDER_STATUS", "ORDER", orderId, null, "/order/" + orderId));
        }

        if (REVIEW_TITLES.contains(title)) {
            return findOrderId(content)
                .map(orderId -> new LegacyTarget("MERCHANT_REVIEW_REPLY", "REVIEW", orderId, null, "/review/" + orderId));
        }

        if (DEAL_ORDER_TITLES.contains(title)) {
            Optional<String> voucherCode = findVoucherCode(content);
            return Optional.of(new LegacyTarget(
                "DEAL_ORDER_STATUS",
                "DEAL_ORDER",
                null,
                voucherCode.orElse(null),
                null
            ));
        }

        return Optional.empty();
    }

    private static Optional<Long> findOrderId(String content) {
        Matcher matcher = ORDER_CONTENT.matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(matcher.group(1)));
    }

    private static Optional<String> findVoucherCode(String content) {
        Matcher matcher = VOUCHER_CONTENT.matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    record LegacyTarget(
        String type,
        String targetType,
        Long primaryId,
        String reference,
        String targetPath
    ) {
    }
}
