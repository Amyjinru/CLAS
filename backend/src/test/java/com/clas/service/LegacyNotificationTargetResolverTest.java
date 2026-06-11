package com.clas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clas.entity.Notification;
import org.junit.jupiter.api.Test;

class LegacyNotificationTargetResolverTest {
    @Test
    void resolvesLegacyOrderNotificationFromContentOrderId() {
        Notification notification = new Notification();
        notification.setTitle("退款已通过");
        notification.setContent("订单 18 已退款。");

        LegacyNotificationTargetResolver.LegacyTarget target =
            LegacyNotificationTargetResolver.resolve(notification).orElseThrow();

        assertEquals("ORDER_STATUS", target.type());
        assertEquals("ORDER", target.targetType());
        assertEquals(18L, target.primaryId());
        assertEquals("/order/18", target.targetPath());
    }

    @Test
    void resolvesLegacyReviewReplyFromOrderContent() {
        Notification notification = new Notification();
        notification.setTitle("商家回复了评价");
        notification.setContent("您的订单 6 评价收到商家回复。");

        LegacyNotificationTargetResolver.LegacyTarget target =
            LegacyNotificationTargetResolver.resolve(notification).orElseThrow();

        assertEquals("MERCHANT_REVIEW_REPLY", target.type());
        assertEquals("REVIEW", target.targetType());
        assertEquals(6L, target.primaryId());
        assertEquals("/review/6", target.targetPath());
    }

    @Test
    void ignoresUnrelatedAnnouncements() {
        Notification notification = new Notification();
        notification.setTitle("平台公告");
        notification.setContent("今晚系统维护。");

        assertTrue(LegacyNotificationTargetResolver.resolve(notification).isEmpty());
    }
}
