package com.clas.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AmapRouteService {
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    private final RestClient restClient;
    private final String webServiceKey;
    private final ConcurrentHashMap<String, CachedRouteEstimate> estimateCache = new ConcurrentHashMap<>();

    public AmapRouteService(@Value("${amap.web-service-key:}") String webServiceKey) {
        this.restClient = RestClient.builder()
            .baseUrl("https://restapi.amap.com")
            .build();
        this.webServiceKey = webServiceKey;
    }

    public Optional<RouteEstimate> estimateDriving(
        BigDecimal originLongitude,
        BigDecimal originLatitude,
        BigDecimal destinationLongitude,
        BigDecimal destinationLatitude
    ) {
        if (webServiceKey == null || webServiceKey.isBlank()) {
            return Optional.empty();
        }
        String cacheKey = cacheKey(originLongitude, originLatitude, destinationLongitude, destinationLatitude);
        CachedRouteEstimate cached = estimateCache.get(cacheKey);
        Instant now = Instant.now();
        if (cached != null && cached.isFresh(now)) {
            return Optional.of(cached.estimate());
        }
        try {
            JsonNode body = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v3/direction/driving")
                    .queryParam("origin", coordinate(originLongitude, originLatitude))
                    .queryParam("destination", coordinate(destinationLongitude, destinationLatitude))
                    .queryParam("extensions", "base")
                    .queryParam("key", webServiceKey)
                    .build())
                .retrieve()
                .body(JsonNode.class);
            if (body == null || !"1".equals(body.path("status").asText())) {
                return Optional.empty();
            }
            JsonNode path = body.path("route").path("paths").path(0);
            int distanceMeters = path.path("distance").asInt(0);
            int durationSeconds = path.path("duration").asInt(0);
            if (distanceMeters <= 0) {
                return Optional.empty();
            }
            int minutes = Math.max(1, (int) Math.ceil(durationSeconds / 60.0));
            RouteEstimate estimate = new RouteEstimate(distanceMeters, minutes);
            estimateCache.put(cacheKey, new CachedRouteEstimate(estimate, now));
            return Optional.of(estimate);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String cacheKey(
        BigDecimal originLongitude,
        BigDecimal originLatitude,
        BigDecimal destinationLongitude,
        BigDecimal destinationLatitude
    ) {
        return rounded(originLongitude) + "_" + rounded(originLatitude) + "_"
            + rounded(destinationLongitude) + "_" + rounded(destinationLatitude);
    }

    private String rounded(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP).toPlainString();
    }

    private String coordinate(BigDecimal longitude, BigDecimal latitude) {
        return longitude.toPlainString() + "," + latitude.toPlainString();
    }

    public record RouteEstimate(int distanceMeters, int durationMinutes) {
    }

    private record CachedRouteEstimate(RouteEstimate estimate, Instant cachedAt) {
        private boolean isFresh(Instant now) {
            return cachedAt.plus(CACHE_TTL).isAfter(now);
        }
    }
}
