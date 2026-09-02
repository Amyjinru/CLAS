package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.GeoUtils;
import com.clas.config.UserContext;
import com.clas.dto.DeliveryTrackingResponse;
import com.clas.entity.Merchant;
import com.clas.entity.Orders;
import com.clas.entity.RiderProfile;
import com.clas.client.MerchantClient;
import com.clas.client.OrderClient;
import com.clas.mapper.RiderProfileMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DeliveryTrackingService {
    private static final Duration LOCATION_STALE_AFTER = Duration.ofMinutes(1);
    private final OrderClient orderClient;
    private final MerchantClient merchantClient;
    private final RiderProfileMapper riders;
    private final AmapRouteService amap;

    public DeliveryTrackingService(OrderClient orderClient, MerchantClient merchantClient, RiderProfileMapper riders, AmapRouteService amap) {
        this.orderClient = orderClient; this.merchantClient = merchantClient; this.riders = riders; this.amap = amap;
    }

    public DeliveryTrackingResponse tracking(Long orderId) {
        Orders order = requireAuthorizedOrder(orderId);
        boolean active = "ASSIGNED_WAITING_MEAL".equals(order.getDeliveryStatus()) || "DELIVERING".equals(order.getDeliveryStatus());
        if (!active) return response(order, null, null, false, "NONE", false);
        RiderProfile rider = riders.selectById(order.getRiderId());
        Merchant merchant = merchantClient.getMerchant(order.getMerchantId());
        Eta eta = estimateEta(order, rider, merchant);
        if (eta.arrival() != null && !eta.arrival().equals(order.getPredictedArrivalAt())) {
            try {
                orderClient.updatePredictedArrival(order.getId(), eta.arrival());
            } catch (BusinessException ignored) {
                // 轨迹查询仍返回本次估算；预计到达回写失败不阻断用户查看。
            }
            order.setPredictedArrivalAt(eta.arrival());
        }
        return response(order, rider, eta.arrival(), eta.routeAvailable(), eta.source(), true);
    }

    private Eta estimateEta(Orders order, RiderProfile rider, Merchant merchant) {
        if (rider == null || merchant == null || !GeoUtils.hasCoordinate(order.getDeliveryLongitude(), order.getDeliveryLatitude())) return new Eta(order.getPredictedArrivalAt(), false, "UNAVAILABLE");
        BigDecimal originLng; BigDecimal originLat; int preparation = 0;
        if ("ASSIGNED_WAITING_MEAL".equals(order.getDeliveryStatus())) {
            originLng = rider.getCurrentLongitude(); originLat = rider.getCurrentLatitude(); preparation = Math.max(0, order.getPrepareMinutesSnapshot() == null ? 0 : order.getPrepareMinutesSnapshot());
            if (!GeoUtils.hasCoordinate(originLng, originLat) || !GeoUtils.hasCoordinate(merchant.getLongitude(), merchant.getLatitude())) return new Eta(order.getPredictedArrivalAt(), false, "UNAVAILABLE");
            RouteLeg toMerchant = estimate(originLng, originLat, merchant.getLongitude(), merchant.getLatitude());
            RouteLeg toUser = estimate(merchant.getLongitude(), merchant.getLatitude(), order.getDeliveryLongitude(), order.getDeliveryLatitude());
            return new Eta(LocalDateTime.now().plusMinutes(toMerchant.minutes() + preparation + toUser.minutes()), toMerchant.amap() && toUser.amap(), toMerchant.amap() && toUser.amap() ? "AMAP" : "STRAIGHT_LINE");
        }
        originLng = rider.getCurrentLongitude(); originLat = rider.getCurrentLatitude();
        if (!GeoUtils.hasCoordinate(originLng, originLat)) return new Eta(order.getPredictedArrivalAt(), false, "UNAVAILABLE");
        RouteLeg toUser = estimate(originLng, originLat, order.getDeliveryLongitude(), order.getDeliveryLatitude());
        return new Eta(LocalDateTime.now().plusMinutes(toUser.minutes()), toUser.amap(), toUser.amap() ? "AMAP" : "STRAIGHT_LINE");
    }

    private RouteLeg estimate(BigDecimal fromLng, BigDecimal fromLat, BigDecimal toLng, BigDecimal toLat) {
        Optional<AmapRouteService.RouteEstimate> route = amap.estimateDriving(fromLng, fromLat, toLng, toLat);
        if (route.isPresent()) return new RouteLeg(route.get().durationMinutes(), true);
        int distance = GeoUtils.distanceMeters(fromLat, fromLng, toLat, toLng);
        return new RouteLeg(Math.max(1, (int) Math.ceil(distance / 250.0)), false);
    }

    private DeliveryTrackingResponse response(Orders order, RiderProfile rider, LocalDateTime predicted, boolean routeAvailable, String source, boolean active) {
        boolean stale = rider == null || rider.getLocationUpdatedAt() == null || rider.getLocationUpdatedAt().plus(LOCATION_STALE_AFTER).isBefore(LocalDateTime.now());
        boolean live = active && !stale && rider != null && GeoUtils.hasCoordinate(rider.getCurrentLongitude(), rider.getCurrentLatitude());
        Integer remaining = predicted == null ? null : Math.max(0, (int) Math.ceil(Duration.between(LocalDateTime.now(), predicted).toSeconds() / 60.0));
        return new DeliveryTrackingResponse(order.getId(), order.getDeliveryStatus(), order.getPromiseStartAt(), order.getPromiseEndAt(), predicted,
            remaining, routeAvailable, source, live, stale, live ? rider.getCurrentLongitude() : null, live ? rider.getCurrentLatitude() : null, live ? rider.getLocationUpdatedAt() : null);
    }

    private Orders requireAuthorizedOrder(Long orderId) {
        Orders order = orderClient.getOrder(orderId);
        if (order == null) throw new BusinessException("订单不存在", DomainErrorCode.RESOURCE_NOT_FOUND);
        String userId = UserContext.getUserId(); String role = UserContext.getRole();
        boolean authorized = "ADMIN".equals(role)
            || ("USER".equals(role) && userId.equals(order.getUserId()))
            || ("RIDER".equals(role) && userId.equals(order.getRiderId()))
            || ("MERCHANT".equals(role) && ownsMerchant(order.getMerchantId(), userId));
        if (!authorized) throw new BusinessException("无权查看该订单的配送追踪", DomainErrorCode.DELIVERY_FORBIDDEN);
        return order;
    }

    private boolean ownsMerchant(Long merchantId, String userId) {
        Merchant merchant = merchantClient.getMerchant(merchantId);
        return merchant != null && userId.equals(merchant.getUserId());
    }

    private record RouteLeg(int minutes, boolean amap) { }
    private record Eta(LocalDateTime arrival, boolean routeAvailable, String source) { }
}
