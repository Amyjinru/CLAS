package com.clas.common;

public class BusinessException extends RuntimeException {
    private final int httpStatus;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

