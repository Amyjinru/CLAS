package com.clas.client;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.Result;
import com.clas.common.client.ServiceEndpoints;
import com.clas.common.dto.InternalTokenValidationRequest;
import com.clas.common.dto.InternalValidatedUser;
import com.clas.dto.AppealProcessRequest;
import com.clas.dto.InternalAddressResponse;
import com.clas.dto.InternalNotificationRequest;
import com.clas.dto.InternalPage;
import com.clas.dto.InternalUserProfile;
import com.clas.dto.InternalUserSummary;
import com.clas.dto.PenaltyRequest;
import com.clas.entity.Appeal;
import com.clas.entity.UserPenalty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class IamClient {
    private final RestTemplate restTemplate;
    private final ServiceEndpoints serviceEndpoints;
    private final ObjectMapper objectMapper;

    public IamClient(RestTemplate restTemplate, ServiceEndpoints serviceEndpoints, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.serviceEndpoints = serviceEndpoints;
        this.objectMapper = objectMapper;
    }

    public InternalUserSummary getUser(String userId) {
        return get("/users/" + userId, new ParameterizedTypeReference<Result<InternalUserSummary>>() {});
    }

    public InternalUserProfile getUserProfile(String userId) {
        return get("/users/" + userId + "/profile", new ParameterizedTypeReference<Result<InternalUserProfile>>() {});
    }

    public Map<String, InternalUserProfile> getUserProfiles(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        String ids = userIds.stream().distinct().collect(Collectors.joining(","));
        List<InternalUserProfile> users = get(
            "/users/batch?ids=" + ids,
            new ParameterizedTypeReference<Result<List<InternalUserProfile>>>() {}
        );
        if (users == null) {
            return Map.of();
        }
        return users.stream().collect(Collectors.toMap(InternalUserProfile::phone, Function.identity(), (a, b) -> a));
    }

    public InternalPage<InternalUserProfile> listUsers(int page, int size, String role, Boolean enabled, String keyword) {
        UriComponentsBuilder builder = uri("/users")
            .queryParam("page", page)
            .queryParam("size", size);
        if (role != null && !role.isBlank()) {
            builder.queryParam("role", role);
        }
        if (enabled != null) {
            builder.queryParam("enabled", enabled);
        }
        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("keyword", keyword);
        }
        InternalPage<InternalUserProfile> result = exchange(
            builder.toUriString(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Result<InternalPage<InternalUserProfile>>>() {}
        );
        return result == null ? new InternalPage<>(List.of(), 0, page, size) : result;
    }

    public List<InternalUserProfile> exportUsers(String role, Boolean enabled, String keyword) {
        UriComponentsBuilder builder = uri("/users/export");
        if (role != null && !role.isBlank()) {
            builder.queryParam("role", role);
        }
        if (enabled != null) {
            builder.queryParam("enabled", enabled);
        }
        if (keyword != null && !keyword.isBlank()) {
            builder.queryParam("keyword", keyword);
        }
        List<InternalUserProfile> users = exchange(
            builder.toUriString(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Result<List<InternalUserProfile>>>() {}
        );
        return users == null ? List.of() : users;
    }

    public List<String> rolesOf(String userId) {
        List<String> roles = get("/users/" + userId + "/roles", new ParameterizedTypeReference<Result<List<String>>>() {});
        return roles == null ? List.of("USER") : roles;
    }

    public void grantRole(String userId, String role) {
        exchange(
            serviceEndpoints.iam() + "/internal/iam/v1/users/" + userId + "/roles/" + role,
            HttpMethod.POST,
            null,
            new ParameterizedTypeReference<Result<Void>>() {}
        );
    }

    public void upsertRoleStatus(String userId, String role, String status) {
        exchange(
            serviceEndpoints.iam() + "/internal/iam/v1/users/" + userId + "/roles/" + role + "/status",
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("status", status)),
            new ParameterizedTypeReference<Result<Void>>() {}
        );
    }

    public InternalAddressResponse getAddress(Long addressId, String userId) {
        return get(
            "/addresses/" + addressId + "?userId=" + userId,
            new ParameterizedTypeReference<Result<InternalAddressResponse>>() {}
        );
    }

    public long favoriteCount(Long merchantId) {
        Long count = get(
            "/merchants/" + merchantId + "/favorite-count",
            new ParameterizedTypeReference<Result<Long>>() {}
        );
        return count == null ? 0L : count;
    }

    public void assertCanUsePlatform(String userId) {
        assertAccess("/users/" + userId + "/platform-access", "当前账号无法使用平台");
    }

    public void assertCanComment(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        assertAccess("/users/" + userId + "/comment-access", "您已被禁言，暂时无法发表评论或评价");
    }

    public boolean hasPendingRiderRoleApplication(String userId) {
        Boolean pending = get(
            "/users/" + userId + "/rider-role-pending",
            new ParameterizedTypeReference<Result<Boolean>>() {}
        );
        return Boolean.TRUE.equals(pending);
    }

    public boolean ownsBankCard(Long cardId, String userId) {
        Boolean owned = get(
            "/bank-cards/" + cardId + "/owned?userId=" + userId,
            new ParameterizedTypeReference<Result<Boolean>>() {}
        );
        return Boolean.TRUE.equals(owned);
    }

    public InternalValidatedUser validateToken(String token) {
        return post(
            "/auth/validate",
            new InternalTokenValidationRequest(token),
            new ParameterizedTypeReference<Result<InternalValidatedUser>>() {}
        );
    }

    public Map<String, Long> getPublicStats() {
        Map<String, Long> stats = get("/stats/public", new ParameterizedTypeReference<Result<Map<String, Long>>>() {});
        return stats == null ? Map.of() : stats;
    }

    public void sendNotification(InternalNotificationRequest request) {
        post("/notifications", request, new ParameterizedTypeReference<Result<Void>>() {});
    }

    public UserPenalty applyPenalty(PenaltyRequest request, String adminId) {
        return exchange(
            uri("/admin/penalties").queryParam("adminId", adminId).toUriString(),
            HttpMethod.POST,
            new HttpEntity<>(request),
            new ParameterizedTypeReference<Result<UserPenalty>>() {}
        );
    }

    public void revokePenalty(Long penaltyId, String adminId) {
        exchange(
            uri("/admin/penalties/" + penaltyId + "/revoke").queryParam("adminId", adminId).toUriString(),
            HttpMethod.POST,
            null,
            new ParameterizedTypeReference<Result<Void>>() {}
        );
    }

    public InternalUserProfile restoreAccount(String userId, String adminId) {
        return exchange(
            uri("/admin/users/" + userId + "/restore").queryParam("adminId", adminId).toUriString(),
            HttpMethod.POST,
            null,
            new ParameterizedTypeReference<Result<InternalUserProfile>>() {}
        );
    }

    public List<Appeal> listPendingAppeals() {
        List<Appeal> appeals = get("/admin/appeals", new ParameterizedTypeReference<Result<List<Appeal>>>() {});
        return appeals == null ? List.of() : appeals;
    }

    public Appeal processAppeal(Long appealId, String status, String adminReply, String adminId) {
        return post(
            "/admin/appeals/" + appealId + "/process",
            new AppealProcessRequest(status, adminReply, adminId),
            new ParameterizedTypeReference<Result<Appeal>>() {}
        );
    }

    private void assertAccess(String path, String fallbackMessage) {
        try {
            restTemplate.exchange(
                serviceEndpoints.iam() + "/internal/iam/v1" + path,
                HttpMethod.GET,
                null,
                Void.class
            );
        } catch (HttpStatusCodeException exception) {
            int status = exception.getStatusCode().value();
            if (status >= 400 && status < 500) {
                throw new BusinessException(status, messageOf(exception, fallbackMessage));
            }
            throw upstreamUnavailable();
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    private UriComponentsBuilder uri(String path) {
        return UriComponentsBuilder.fromHttpUrl(serviceEndpoints.iam() + "/internal/iam/v1" + path);
    }

    private <T> T get(String path, ParameterizedTypeReference<Result<T>> type) {
        return exchange(serviceEndpoints.iam() + "/internal/iam/v1" + path, HttpMethod.GET, null, type);
    }

    private <T> T post(String path, Object body, ParameterizedTypeReference<Result<T>> type) {
        return exchange(
            serviceEndpoints.iam() + "/internal/iam/v1" + path,
            HttpMethod.POST,
            new HttpEntity<>(body),
            type
        );
    }

    private <T> T exchange(String url, HttpMethod method, HttpEntity<?> entity, ParameterizedTypeReference<Result<T>> type) {
        try {
            ResponseEntity<Result<T>> response = restTemplate.exchange(url, method, entity, type);
            Result<T> body = response.getBody();
            return body == null ? null : body.data();
        } catch (HttpStatusCodeException exception) {
            throw mapped(exception);
        } catch (RestClientException exception) {
            throw upstreamUnavailable();
        }
    }

    private BusinessException mapped(HttpStatusCodeException exception) {
        int status = exception.getStatusCode().value();
        String message = "账户服务暂不可用，请稍后重试";
        String errorCode = DomainErrorCode.UPSTREAM_UNAVAILABLE;
        try {
            JsonNode node = objectMapper.readTree(exception.getResponseBodyAsString());
            if (node != null) {
                if (node.path("message").isTextual() && !node.path("message").asText().isBlank()) {
                    message = node.path("message").asText();
                }
                if (node.path("errorCode").isTextual() && !node.path("errorCode").asText().isBlank()) {
                    errorCode = node.path("errorCode").asText();
                }
            }
        } catch (Exception ignored) {
        }
        if (status >= 400 && status < 500) {
            return new BusinessException(status, message, errorCode);
        }
        return upstreamUnavailable();
    }

    private String messageOf(HttpStatusCodeException exception, String fallbackMessage) {
        try {
            JsonNode node = objectMapper.readTree(exception.getResponseBodyAsString());
            if (node != null && node.path("message").isTextual() && !node.path("message").asText().isBlank()) {
                return node.path("message").asText();
            }
        } catch (Exception ignored) {
        }
        return fallbackMessage;
    }

    private BusinessException upstreamUnavailable() {
        return new BusinessException(503, "账户服务暂不可用，请稍后重试", DomainErrorCode.UPSTREAM_UNAVAILABLE);
    }
}
