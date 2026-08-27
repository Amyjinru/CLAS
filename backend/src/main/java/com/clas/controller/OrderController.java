package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.CreateOrderRequest;
import com.clas.dto.CreateOrderBatchRequest;
import com.clas.dto.CreateOrderBatchResponse;
import com.clas.dto.OrderPreviewResponse;
import com.clas.dto.OrderResponse;
import com.clas.dto.OrderLifecycleEventResponse;
import com.clas.dto.PaymentRequest;
import com.clas.dto.PaymentResponse;
import com.clas.dto.RefundRequest;
import com.clas.dto.RefundResolveRequest;
import com.clas.dto.RefundDisputeRequest;
import com.clas.dto.RejectOrderRequest;
import com.clas.dto.RiderTipRequest;
import com.clas.dto.RiderReviewRequest;
import com.clas.entity.Orders;
import com.clas.entity.RiderTip;
import com.clas.entity.RiderReview;
import com.clas.service.MerchantService;
import com.clas.service.OrderService;
import com.clas.service.PaymentService;
import com.clas.service.RiderTipService;
import com.clas.service.RiderReviewService;
import com.clas.service.OrderLifecycleService;
import com.clas.service.OrderRefundDisputeService;
import com.clas.service.RefundResolutionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final MerchantService merchantService;
    private final RiderTipService riderTipService;
    private final RiderReviewService riderReviewService;
    private final OrderLifecycleService lifecycleService;
    private final OrderRefundDisputeService refundDisputeService;
    private final RefundResolutionService refundResolutionService;

    public OrderController(OrderService orderService, PaymentService paymentService, MerchantService merchantService, RiderTipService riderTipService, RiderReviewService riderReviewService, OrderLifecycleService lifecycleService, OrderRefundDisputeService refundDisputeService, RefundResolutionService refundResolutionService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.merchantService = merchantService;
        this.riderTipService = riderTipService;
        this.riderReviewService = riderReviewService;
        this.lifecycleService = lifecycleService;
        this.refundDisputeService = refundDisputeService;
        this.refundResolutionService = refundResolutionService;
    }

    @PostMapping("/create")
    @RequireRole("USER")
    public Result<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        return Result.ok(orderService.create(new CreateOrderRequest(
            currentUserId(),
            request.merchantId(),
            request.addressId(),
            request.deliveryAddress(),
            request.remark(),
            request.userCouponId(),
            request.productIds()
        )));
    }

    @PostMapping("/create-batch")
    @RequireRole("USER")
    public Result<CreateOrderBatchResponse> createBatch(@Valid @RequestBody CreateOrderBatchRequest request) {
        return Result.ok(orderService.createBatch(currentUserId(), request));
    }

    @GetMapping("/preview")
    @RequireRole("USER")
    public Result<OrderPreviewResponse> preview(
        @RequestParam Long merchantId,
        @RequestParam(required = false) Long addressId,
        @RequestParam(required = false) Long userCouponId,
        @RequestParam(required = false) List<Long> productIds
    ) {
        return Result.ok(orderService.previewCheckout(currentUserId(), merchantId, addressId, userCouponId, productIds));
    }

    @GetMapping("/list/{userId}")
    @Deprecated
    @RequireRole("USER")
    public Result<List<OrderResponse>> list(@PathVariable String userId) {
        return listMine();
    }

    @GetMapping("/me")
    @RequireRole("USER")
    public Result<List<OrderResponse>> listMine() {
        return Result.ok(orderService.listForUser(currentUserId()));
    }

    @GetMapping("/{orderId}")
    @RequireRole("USER")
    public Result<OrderResponse> detail(@PathVariable Long orderId) {
        return Result.ok(orderService.getForUser(orderId, currentUserId()));
    }

    @GetMapping("/merchant/{merchantId}")
    @Deprecated
    @RequireRole("MERCHANT")
    public Result<List<OrderResponse>> merchantOrders(@PathVariable Long merchantId) {
        return myMerchantOrders();
    }

    @GetMapping("/merchant/me")
    @RequireRole("MERCHANT")
    public Result<List<OrderResponse>> myMerchantOrders() {
        return Result.ok(orderService.listForMerchant(merchantService.getCurrentMerchantId()));
    }

    @GetMapping("/merchant/me/user/{userId}")
    @RequireRole("MERCHANT")
    public Result<List<OrderResponse>> myMerchantOrdersByUser(@PathVariable String userId) {
        return Result.ok(orderService.listForMerchantAndUser(merchantService.getCurrentMerchantId(), userId));
    }

    @GetMapping("/merchant/detail/{orderId}")
    @RequireRole("MERCHANT")
    public Result<OrderResponse> myMerchantOrderDetail(@PathVariable Long orderId) {
        return Result.ok(orderService.getForMerchant(orderId, merchantService.getCurrentMerchantId()));
    }

    @GetMapping("/admin/{orderId}")
    @RequireRole("ADMIN")
    public Result<OrderResponse> adminOrderDetail(@PathVariable Long orderId) {
        return Result.ok(orderService.getForAdmin(orderId));
    }

    @PostMapping("/pay/{orderId}")
    @RequireRole("USER")
    public Result<PaymentResponse> pay(
        @PathVariable Long orderId,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        PaymentRequest request = new PaymentRequest(orderId, currentUserId(), "MOCK", idempotencyKey);
        return Result.ok(paymentService.mockPay(request));
    }

    @PostMapping("/accept/{orderId}")
    @RequireRole("MERCHANT")
    public Result<Orders> accept(@PathVariable Long orderId) {
        return Result.ok(orderService.accept(orderId, merchantService.getCurrentMerchantId()));
    }

    @PostMapping("/ready-for-dispatch/{orderId}")
    @RequireRole("MERCHANT")
    public Result<Orders> readyForDispatch(@PathVariable Long orderId) {
        return Result.ok(orderService.readyForDispatch(orderId, merchantService.getCurrentMerchantId()));
    }

    @GetMapping("/{orderId}/timeline")
    @RequireRole({"USER", "MERCHANT", "RIDER", "ADMIN"})
    public Result<List<OrderLifecycleEventResponse>> timeline(@PathVariable Long orderId) {
        Orders order = orderService.requireOrder(orderId);
        String role = UserContext.getRole();
        if ("USER".equals(role)) orderService.requireUserOrder(orderId, currentUserId());
        else if ("MERCHANT".equals(role)) orderService.requireMerchantOrder(orderId, merchantService.getCurrentMerchantId());
        else if ("RIDER".equals(role) && !currentUserId().equals(order.getRiderId())) {
            throw new com.clas.common.BusinessException("只能查看本人配送订单的进度", com.clas.common.DomainErrorCode.AUTH_FORBIDDEN);
        }
        return Result.ok(lifecycleService.list(orderId));
    }

    @PostMapping("/complete/{orderId}")
    @RequireRole("USER")
    public Result<Orders> complete(@PathVariable Long orderId) {
        return Result.ok(orderService.complete(orderId, currentUserId()));
    }
    @PostMapping("/{orderId}/rider-tip") @RequireRole("USER")
    public Result<RiderTip> tip(@PathVariable Long orderId, @Valid @RequestBody RiderTipRequest request) { return Result.ok(riderTipService.pay(orderService.requireUserOrder(orderId, currentUserId()), currentUserId(), request.amount(), request.idempotencyKey())); }
    @PostMapping("/{orderId}/rider-review") @RequireRole("USER") public Result<RiderReview> riderReview(@PathVariable Long orderId,@Valid @RequestBody RiderReviewRequest request){return Result.ok(riderReviewService.create(orderService.requireUserOrder(orderId,currentUserId()),currentUserId(),request.score(),request.tags(),request.content()));}
    @GetMapping("/{orderId}/rider-review") @RequireRole("USER") public Result<RiderReview> riderReviewDetail(@PathVariable Long orderId) { return Result.ok(riderReviewService.getForOrder(orderService.requireUserOrder(orderId, currentUserId()))); }

    @PostMapping("/cancel/{orderId}")
    @RequireRole("USER")
    public Result<Orders> cancel(@PathVariable Long orderId) {
        return Result.ok(orderService.cancel(orderId, currentUserId()));
    }

    @PostMapping("/reject/{orderId}")
    @RequireRole("MERCHANT")
    public Result<Orders> reject(@PathVariable Long orderId, @Valid @RequestBody RejectOrderRequest request) {
        return Result.ok(orderService.reject(orderId, merchantService.getCurrentMerchantId(), request.reason()));
    }

    @PostMapping("/deliver/{orderId}")
    @RequireRole("MERCHANT")
    public Result<Orders> deliver(@PathVariable Long orderId) {
        return Result.ok(orderService.deliver(orderId, merchantService.getCurrentMerchantId()));
    }

    @PostMapping("/refund/{orderId}")
    @RequireRole("USER")
    public Result<Orders> refund(@PathVariable Long orderId, @Valid @RequestBody(required = false) RefundRequest request) {
        String reason = request == null ? "用户申请退款" : request.reason();
        return Result.ok(orderService.requestRefund(orderId, currentUserId(), reason));
    }

    @PostMapping("/refund/{orderId}/approve")
    @RequireRole("MERCHANT")
    public Result<Orders> approveRefund(@PathVariable Long orderId) {
        return Result.ok(orderService.resolveRefund(orderId, merchantService.getCurrentMerchantId(), true));
    }

    @PostMapping("/refund/{orderId}/reject")
    @RequireRole("MERCHANT")
    public Result<Orders> rejectRefund(
        @PathVariable Long orderId,
        @RequestBody(required = false) RefundResolveRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return Result.ok(refundResolutionService.resolveByMerchant(orderId, merchantService.getCurrentMerchantId(), false, reason));
    }

    @PostMapping("/refund/{orderId}/dispute")
    @RequireRole("USER")
    public Result<com.clas.entity.OrderRefundDispute> submitRefundDispute(
        @PathVariable Long orderId, @Valid @RequestBody RefundDisputeRequest request
    ) {
        return Result.ok(refundDisputeService.submit(orderId, currentUserId(), request.reason()));
    }

    private String currentUserId() {
        return UserContext.getUserId();
    }
}
