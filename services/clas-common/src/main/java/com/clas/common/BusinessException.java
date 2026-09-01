package com.clas.common;

public class BusinessException extends RuntimeException {
    private final int httpStatus;
    private final String errorCode;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int httpStatus, String message) {
        this(httpStatus, message, null);
    }

    public BusinessException(String message, String errorCode) {
        this(400, message, errorCode);
    }

    public BusinessException(int httpStatus, String message, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
