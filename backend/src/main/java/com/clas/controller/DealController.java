package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.DealRequest;
import com.clas.entity.DealOrder;
import com.clas.entity.GroupDeal;
import com.clas.service.DealService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deals")
public class DealController {
    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @GetMapping
    public Result<List<GroupDeal>> list(@RequestParam(required = false) Long merchantId) {
        return Result.ok(dealService.list(merchantId));
    }

    @GetMapping("/merchant")
    @RequireRole("MERCHANT")
    public Result<List<GroupDeal>> merchantDeals() {
        return Result.ok(dealService.merchantDeals());
    }

    @PostMapping("/merchant")
    @RequireRole("MERCHANT")
    public Result<GroupDeal> create(@Valid @RequestBody DealRequest request) {
        return Result.ok(dealService.create(request));
    }

    @PostMapping("/{dealId}/buy")
    @RequireRole("USER")
    public Result<DealOrder> buy(@PathVariable Long dealId) {
        return Result.ok(dealService.buy(dealId));
    }

    @GetMapping("/mine")
    @RequireRole("USER")
    public Result<List<DealOrder>> myOrders() {
        return Result.ok(dealService.myOrders());
    }

    @PostMapping("/redeem")
    @RequireRole("MERCHANT")
    public Result<DealOrder> redeem(@RequestBody Map<String, String> body) {
        return Result.ok(dealService.redeem(body.get("voucherCode")));
    }
}
