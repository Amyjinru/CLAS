package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.PaymentRequest;
import com.clas.dto.PaymentResponse;
import com.clas.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/mock")
    public Result<PaymentResponse> mockPay(@Valid @RequestBody PaymentRequest request) {
        return Result.ok(paymentService.mockPay(request));
    }

    @GetMapping("/status/{orderId}")
    public Result<PaymentResponse> status(@PathVariable Long orderId) {
        return Result.ok(paymentService.getPaymentStatus(orderId));
    }
}
