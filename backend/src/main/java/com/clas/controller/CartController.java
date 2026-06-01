package com.clas.controller;

import com.clas.common.Result;
import com.clas.dto.AddCartRequest;
import com.clas.dto.CartItemResponse;
import com.clas.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public Result<List<CartItemResponse>> add(@Valid @RequestBody AddCartRequest request) {
        return Result.ok(cartService.add(request));
    }

    @GetMapping("/list/{userId}")
    public Result<List<CartItemResponse>> list(@PathVariable Long userId) {
        return Result.ok(cartService.list(userId));
    }

    @DeleteMapping("/clear/{userId}")
    public Result<Void> clear(@PathVariable Long userId) {
        cartService.clear(userId);
        return Result.ok();
    }
}

