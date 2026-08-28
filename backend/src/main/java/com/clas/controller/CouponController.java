package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.CouponResponse;
import com.clas.dto.UserCouponResponse;
import com.clas.service.CouponService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {
    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/claimable")
    @RequireRole("USER")
    public Result<List<CouponResponse>> claimable() {
        return Result.ok(couponService.listClaimable(UserContext.getUserId()));
    }

    @PostMapping("/claim/{couponId}")
    @RequireRole("USER")
    public Result<UserCouponResponse> claim(@PathVariable Long couponId) {
        return Result.ok(couponService.claim(UserContext.getUserId(), couponId));
    }

    @GetMapping("/mine")
    @RequireRole("USER")
    public Result<List<UserCouponResponse>> mine() {
        return Result.ok(couponService.listMine(UserContext.getUserId()));
    }
}
