package com.clas.common;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return Result.fail(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("请求参数错误");
        return Result.fail(message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        // 普通异常不直接暴露堆栈或 SQL 等内部信息，前端只需要稳定的兜底提示。
        return Result.fail("系统异常，请稍后重试");
    }
}
