package com.clas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.MerchantStatusEnum;
import com.clas.common.Result;
import com.clas.dto.MerchantScoreUpdateRequest;
import com.clas.dto.RoleApplicationRecordResponse;
import com.clas.entity.Merchant;
import com.clas.mapper.MerchantMapper;
import com.clas.service.InternalMerchantService;
import com.clas.service.MerchantService;
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
@RequestMapping("/internal/merchant/v1")
public class InternalMerchantController {
    private final InternalMerchantService internalMerchantService;
    private final MerchantService merchantService;
    private final MerchantMapper merchantMapper;

    public InternalMerchantController(
        InternalMerchantService internalMerchantService,
        MerchantService merchantService,
        MerchantMapper merchantMapper
    ) {
        this.internalMerchantService = internalMerchantService;
        this.merchantService = merchantService;
        this.merchantMapper = merchantMapper;
    }

    @GetMapping("/merchants/{merchantId}")
    public Result<Merchant> getMerchant(@PathVariable Long merchantId) {
        return Result.ok(internalMerchantService.getById(merchantId));
    }

    @GetMapping("/merchants/batch")
    public Result<List<Merchant>> getMerchantsBatch(@RequestParam("ids") String ids) {
        return Result.ok(internalMerchantService.getByIds(internalMerchantService.parseIds(ids)));
    }

    @GetMapping("/users/{userId}/merchant-pending")
    public Result<Boolean> hasPendingMerchantApplication(@PathVariable String userId) {
        return Result.ok(internalMerchantService.hasPendingApplication(userId));
    }

    @GetMapping("/users/{userId}/merchant-application-record")
    public Result<RoleApplicationRecordResponse> getMerchantApplicationRecord(@PathVariable String userId) {
        return Result.ok(internalMerchantService.getApplicationRecord(userId));
    }

    @GetMapping("/users/{userId}/merchant-id")
    public Result<Long> getMerchantIdByUser(@PathVariable String userId) {
        var merchant = merchantService.getMerchantByUserId(userId);
        return Result.ok(merchant == null ? null : merchant.id());
    }

    @PostMapping("/merchants/{merchantId}/refresh-average-price")
    public Result<Void> refreshAveragePrice(@PathVariable Long merchantId) {
        merchantService.refreshAveragePrice(merchantId);
        return Result.ok();
    }

    @PostMapping("/merchants/{merchantId}/score")
    public Result<Void> updateMerchantScore(@PathVariable Long merchantId, @RequestBody MerchantScoreUpdateRequest request) {
        internalMerchantService.updateScore(merchantId, request.score());
        return Result.ok();
    }

    @GetMapping("/stats/public")
    public Result<Map<String, Long>> publicStats() {
        long merchants = merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
            .eq(Merchant::getStatus, MerchantStatusEnum.OPEN));
        return Result.ok(Map.of("merchants", merchants));
    }

    @GetMapping("/stats/admin")
    public Result<Map<String, Long>> adminStats() {
        return Result.ok(Map.of("merchants", internalMerchantService.countAll()));
    }

    @GetMapping("/merchants/top-by-score")
    public Result<List<Merchant>> topByScore(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(internalMerchantService.topByScore(limit));
    }
}
