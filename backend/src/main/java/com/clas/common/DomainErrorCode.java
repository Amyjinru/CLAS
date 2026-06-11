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
    public static final String ORDER_INVALID_STATE = "ORDER_INVALID_STATE";
    public static final String PAYMENT_IDEMPOTENCY_CONFLICT = "PAYMENT_IDEMPOTENCY_CONFLICT";
    public static final String STOCK_NOT_ENOUGH = "STOCK_NOT_ENOUGH";

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
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("幂等键")) {
            return PAYMENT_IDEMPOTENCY_CONFLICT;
        }
        if (message.contains("状态错误") || message.contains("不可支付") || message.contains("当前状态")) {
            return ORDER_INVALID_STATE;
        }
        if (message.contains("库存不足") || message.contains("已被领完")) {
            return STOCK_NOT_ENOUGH;
        }
        if (message.contains("不存在") || message.contains("无权操作") || message.contains("无权访问")) {
            return RESOURCE_NOT_FOUND;
        }
        return BUSINESS_ERROR;
    }
}
