package com.clas.catalog.api;

import com.clas.catalog.service.CatalogNotFoundException;
import com.clas.catalog.service.InsufficientStockException;
import com.clas.catalog.service.InternalAuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CatalogExceptionHandler {
    @ExceptionHandler(InternalAuthenticationException.class)
    ResponseEntity<ApiResponse<Void>> unauthenticated(InternalAuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.failure(401, exception.getMessage()));
    }

    @ExceptionHandler(CatalogNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> notFound(CatalogNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(404, exception.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    ResponseEntity<ApiResponse<Void>> conflict(InsufficientStockException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(409, exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> invalidRequest(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(400, "invalid catalog request"));
    }
}
