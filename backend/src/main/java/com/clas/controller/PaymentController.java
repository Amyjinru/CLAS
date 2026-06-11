package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.PaymentRequest;
import com.clas.dto.PaymentResponse;
import com.clas.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@RequireRole("USER")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/mock")
    public Result<PaymentResponse> mockPay(
        @Valid @RequestBody PaymentRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        String resolvedKey = idempotencyKey == null || idempotencyKey.isBlank()
            ? request.idempotencyKey()
            : idempotencyKey;
        return Result.ok(paymentService.mockPay(new PaymentRequest(
            request.orderId(),
            UserContext.getUserId(),
            request.payMethod(),
            resolvedKey
        )));
    }

    @GetMapping("/status/{orderId}")
    public Result<PaymentResponse> status(@PathVariable Long orderId) {
        return Result.ok(paymentService.getPaymentStatus(orderId, UserContext.getUserId()));
    }
}
