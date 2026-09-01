package com.clas.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        Result<Void> result = new Result<>(
            exception.getHttpStatus(),
            exception.getMessage(),
            null,
            DomainErrorCode.fromBusinessException(exception)
        );
        return ResponseEntity.status(exception.getHttpStatus()).body(result);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .orElse("请求参数错误");
        return ResponseEntity.status(400).body(new Result<>(400, message, null, DomainErrorCode.VALIDATION_ERROR));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
            .findFirst()
            .map(v -> v.getPropertyPath() + " " + v.getMessage())
            .orElse("请求参数错误");
        return ResponseEntity.status(400).body(new Result<>(400, message, null, DomainErrorCode.VALIDATION_ERROR));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(400).body(new Result<>(
            400,
            "请求体格式错误，请检查JSON格式",
            null,
            DomainErrorCode.REQUEST_BODY_INVALID
        ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(405).body(new Result<>(
            405,
            "不支持的请求方法: " + exception.getMethod(),
            null,
            DomainErrorCode.METHOD_NOT_SUPPORTED
        ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException exception) {
        return ResponseEntity.status(400).body(new Result<>(
            400,
            "缺少必要参数: " + exception.getParameterName(),
            null,
            DomainErrorCode.MISSING_PARAMETER
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(500).body(new Result<>(500, "系统异常，请稍后重试", null, DomainErrorCode.SYSTEM_ERROR));
    }
}
