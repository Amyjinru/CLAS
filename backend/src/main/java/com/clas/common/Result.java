package com.clas.common;

public record Result<T>(int code, String message, T data, long timestamp, String requestId) {
    public Result(int code, String message, T data) {
        this(code, message, data, System.currentTimeMillis(), RequestTraceContext.currentRequestId());
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    public static Result<Void> ok() {
        return new Result<>(200, "success", null);
    }

    public static Result<Void> fail(String message) {
        return new Result<>(400, message, null);
    }
}

