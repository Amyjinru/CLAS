package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.DealRequest;
import com.clas.entity.GroupDeal;
import com.clas.service.DealPublishService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deals")
public class DealPublishController {
    private final DealPublishService dealPublishService;

    public DealPublishController(DealPublishService dealPublishService) {
        this.dealPublishService = dealPublishService;
    }

    @GetMapping
    public Result<List<GroupDeal>> list(@RequestParam(required = false) Long merchantId) {
        return Result.ok(dealPublishService.list(merchantId));
    }

    @GetMapping("/{dealId}")
    public Result<GroupDeal> detail(@PathVariable Long dealId) {
        return Result.ok(dealPublishService.getById(dealId));
    }

    @GetMapping("/merchant")
    @RequireRole("MERCHANT")
    public Result<List<GroupDeal>> merchantDeals() {
        return Result.ok(dealPublishService.merchantDeals());
    }

    @PostMapping("/merchant")
    @RequireRole("MERCHANT")
    public Result<GroupDeal> create(@Valid @RequestBody DealRequest request) {
        return Result.ok(dealPublishService.create(request));
    }

    @PutMapping("/merchant/{dealId}")
    @RequireRole("MERCHANT")
    public Result<GroupDeal> update(@PathVariable Long dealId, @Valid @RequestBody DealRequest request) {
        return Result.ok(dealPublishService.updateMerchantDeal(dealId, request));
    }
}
