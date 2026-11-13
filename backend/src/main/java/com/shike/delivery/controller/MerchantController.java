package com.shike.delivery.controller;

import com.shike.delivery.common.ApiResponse;
import com.shike.delivery.domain.DomainEnums.OrderStatus;
import com.shike.delivery.domain.Product;
import com.shike.delivery.domain.ProductCategory;
import com.shike.delivery.security.SecurityUtils;
import com.shike.delivery.service.MerchantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(merchantService.dashboard(SecurityUtils.merchantId()));
    }

    @GetMapping("/orders")
    public ApiResponse<List<Map<String, Object>>> orders(
        @RequestParam(required = false) OrderStatus status
    ) {
        return ApiResponse.ok(merchantService.orders(SecurityUtils.merchantId(), status));
    }

    @PostMapping("/orders/{orderId}/accept")
    public ApiResponse<Map<String, Object>> accept(@PathVariable Long orderId) {
        return ApiResponse.ok(merchantService.accept(SecurityUtils.merchantId(), orderId));
    }

    @PostMapping("/orders/{orderId}/reject")
    public ApiResponse<Map<String, Object>> reject(
        @PathVariable Long orderId,
        @RequestBody(required = false) ReasonRequest request
    ) {
        String reason = request == null ? "商家无法接单" : request.reason();
        return ApiResponse.ok(merchantService.reject(SecurityUtils.merchantId(), orderId, reason));
    }

    @PostMapping("/orders/{orderId}/ready")
    public ApiResponse<Map<String, Object>> ready(@PathVariable Long orderId) {
        return ApiResponse.ok(merchantService.ready(SecurityUtils.merchantId(), orderId));
    }

    @PatchMapping("/store/status")
    public ApiResponse<Map<String, Object>> toggleStore(@RequestBody OpenRequest request) {
        return ApiResponse.ok(merchantService.toggleStore(SecurityUtils.merchantId(), request.open()));
    }

    @GetMapping("/menu")
    public ApiResponse<Map<String, Object>> menu() {
        return ApiResponse.ok(merchantService.menu(SecurityUtils.merchantId()));
    }

    @PostMapping("/categories")
    public ApiResponse<ProductCategory> createCategory(@RequestBody MerchantService.CategoryInput input) {
        return ApiResponse.ok(merchantService.createCategory(SecurityUtils.merchantId(), input));
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<ProductCategory> updateCategory(
        @PathVariable Long categoryId,
        @RequestBody MerchantService.CategoryInput input
    ) {
        return ApiResponse.ok(
            merchantService.updateCategory(SecurityUtils.merchantId(), categoryId, input)
        );
    }

    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long categoryId) {
        merchantService.deleteCategory(SecurityUtils.merchantId(), categoryId);
        return ApiResponse.ok("已删除", null);
    }

    @PostMapping("/products")
    public ApiResponse<Product> createProduct(@RequestBody MerchantService.ProductInput input) {
        return ApiResponse.ok(merchantService.createProduct(SecurityUtils.merchantId(), input));
    }

    @PutMapping("/products/{productId}")
    public ApiResponse<Product> updateProduct(
        @PathVariable Long productId,
        @RequestBody MerchantService.ProductInput input
    ) {
        return ApiResponse.ok(
            merchantService.updateProduct(SecurityUtils.merchantId(), productId, input)
        );
    }

    @PatchMapping("/products/{productId}/sold-out")
    public ApiResponse<Product> soldOut(
        @PathVariable Long productId,
        @RequestBody SoldOutRequest request
    ) {
        return ApiResponse.ok(
            merchantService.toggleSoldOut(SecurityUtils.merchantId(), productId, request.soldOut())
        );
    }

    @DeleteMapping("/products/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long productId) {
        merchantService.deleteProduct(SecurityUtils.merchantId(), productId);
        return ApiResponse.ok("已删除", null);
    }

    public record ReasonRequest(String reason) {
    }

    public record OpenRequest(boolean open) {
    }

    public record SoldOutRequest(boolean soldOut) {
    }
}
