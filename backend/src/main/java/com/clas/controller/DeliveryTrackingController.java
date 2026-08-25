package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.DeliveryTrackingResponse;
import com.clas.service.DeliveryTrackingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryTrackingController {
    private final DeliveryTrackingService tracking;
    public DeliveryTrackingController(DeliveryTrackingService tracking) { this.tracking = tracking; }
    @GetMapping("/orders/{orderId}/tracking")
    @RequireRole({"USER", "RIDER", "MERCHANT", "ADMIN"})
    public Result<DeliveryTrackingResponse> tracking(@PathVariable Long orderId) { return Result.ok(tracking.tracking(orderId)); }
}
