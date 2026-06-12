package com.clas.common;

import java.util.UUID;

public final class RequestTraceContext {
    public static final String HEADER_NAME = "X-Request-Id";

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestTraceContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(normalize(requestId));
    }

    public static String currentRequestId() {
        String requestId = REQUEST_ID.get();
        if (requestId == null || requestId.isBlank()) {
            return newRequestId();
        }
        return requestId;
    }

    public static String normalize(String requestId) {
        if (requestId == null) {
            return newRequestId();
        }
        String normalized = requestId.trim();
        if (normalized.isBlank() || normalized.length() > 128) {
            return newRequestId();
        }
        return normalized;
    }

    public static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
