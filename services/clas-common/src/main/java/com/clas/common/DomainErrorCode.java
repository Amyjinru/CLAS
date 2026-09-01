package com.clas.common;

public final class DomainErrorCode {
    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String AUTH_UNAUTHORIZED = "AUTH_UNAUTHORIZED";
    public static final String AUTH_FORBIDDEN = "AUTH_FORBIDDEN";
    public static final String REQUEST_BODY_INVALID = "REQUEST_BODY_INVALID";
    public static final String METHOD_NOT_SUPPORTED = "METHOD_NOT_SUPPORTED";
    public static final String MISSING_PARAMETER = "MISSING_PARAMETER";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String UPSTREAM_UNAVAILABLE = "UPSTREAM_UNAVAILABLE";
    public static final String DELIVERY_FORBIDDEN = "DELIVERY_FORBIDDEN";
    public static final String DELIVERY_STATE_INVALID = "DELIVERY_STATE_INVALID";
    public static final String RIDER_CAPACITY_REACHED = "RIDER_CAPACITY_REACHED";
    public static final String DELIVERY_TASK_UNAVAILABLE = "DELIVERY_TASK_UNAVAILABLE";

    private DomainErrorCode() {
    }

    public static String fromBusinessException(BusinessException exception) {
        if (exception.getErrorCode() != null && !exception.getErrorCode().isBlank()) {
            return exception.getErrorCode();
        }
        if (exception.getHttpStatus() == 401) {
            return AUTH_UNAUTHORIZED;
        }
        if (exception.getHttpStatus() == 403) {
            return AUTH_FORBIDDEN;
        }
        if (exception.getHttpStatus() == 503) {
            return UPSTREAM_UNAVAILABLE;
        }
        return BUSINESS_ERROR;
    }
}
