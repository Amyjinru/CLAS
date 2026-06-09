package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.AddCartRequest;
import com.clas.dto.CartValidationResponse;
import com.clas.dto.CartItemResponse;
import com.clas.dto.RemoveCartRequest;
import com.clas.dto.UpdateCartRequest;
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
@RequireRole("USER")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public Result<List<CartItemResponse>> add(@Valid @RequestBody AddCartRequest request) {
        return Result.ok(cartService.add(new AddCartRequest(currentUserId(), request.productId(), request.quantity())));
    }

    @PostMapping("/remove")
    public Result<List<CartItemResponse>> remove(@Valid @RequestBody RemoveCartRequest request) {
        return Result.ok(cartService.remove(new RemoveCartRequest(currentUserId(), request.productId(), request.quantity())));
    }

    @PostMapping("/update")
    public Result<List<CartItemResponse>> update(@Valid @RequestBody UpdateCartRequest request) {
        return Result.ok(cartService.update(new UpdateCartRequest(currentUserId(), request.productId(), request.quantity())));
    }

    @GetMapping("/list/{userId}")
    public Result<List<CartItemResponse>> list(@PathVariable String userId) {
        return Result.ok(cartService.list(currentUserId()));
    }

    @DeleteMapping("/item/{userId}/{productId}")
    public Result<List<CartItemResponse>> deleteItem(@PathVariable String userId, @PathVariable Long productId) {
        return Result.ok(cartService.deleteItem(currentUserId(), productId));
    }

    @GetMapping("/validate/{userId}")
    public Result<CartValidationResponse> validate(@PathVariable String userId) {
        return Result.ok(cartService.validate(currentUserId()));
    }

    @DeleteMapping("/invalid/{userId}")
    public Result<List<CartItemResponse>> clearInvalid(@PathVariable String userId) {
        return Result.ok(cartService.clearInvalid(currentUserId()));
    }

    @DeleteMapping("/clear/{userId}")
    public Result<Void> clear(@PathVariable String userId) {
        cartService.clear(currentUserId());
        return Result.ok();
    }

    private String currentUserId() {
        return UserContext.getUserId();
    }
}
