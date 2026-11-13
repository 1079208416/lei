package com.shike.delivery.controller;

import com.shike.delivery.common.ApiResponse;
import com.shike.delivery.security.SecurityUtils;
import com.shike.delivery.service.CartService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user/cart")
@PreAuthorize("hasRole('USER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> view(@RequestParam Long merchantId) {
        return ApiResponse.ok(cartService.view(SecurityUtils.accountId(), merchantId));
    }

    @PostMapping("/items")
    public ApiResponse<Map<String, Object>> add(@RequestBody CartService.AddCartItemRequest request) {
        return ApiResponse.ok(cartService.add(SecurityUtils.accountId(), request));
    }

    @PatchMapping("/items/{itemId}")
    public ApiResponse<Map<String, Object>> quantity(
        @PathVariable Long itemId,
        @RequestBody QuantityRequest request
    ) {
        return ApiResponse.ok(
            cartService.updateQuantity(SecurityUtils.accountId(), itemId, request.quantity())
        );
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Map<String, Object>> remove(@PathVariable Long itemId) {
        return ApiResponse.ok(cartService.remove(SecurityUtils.accountId(), itemId));
    }

    public record QuantityRequest(int quantity) {
    }
}
