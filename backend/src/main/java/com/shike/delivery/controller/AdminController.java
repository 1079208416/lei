package com.shike.delivery.controller;

import com.shike.delivery.common.ApiResponse;
import com.shike.delivery.domain.DomainEnums.OrderStatus;
import com.shike.delivery.domain.DomainEnums.RefundStatus;
import com.shike.delivery.service.AdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(adminService.dashboard());
    }

    @GetMapping("/orders")
    public ApiResponse<List<Map<String, Object>>> orders(
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(adminService.orders(status, keyword));
    }

    @GetMapping("/riders")
    public ApiResponse<List<Map<String, Object>>> riders() {
        return ApiResponse.ok(adminService.riders());
    }

    @PostMapping("/orders/{orderId}/assign")
    public ApiResponse<Map<String, Object>> assign(
        @PathVariable Long orderId,
        @RequestBody AssignRequest request
    ) {
        return ApiResponse.ok(adminService.assign(orderId, request.riderId()));
    }

    @GetMapping("/refunds")
    public ApiResponse<List<Map<String, Object>>> refunds(
        @RequestParam(required = false) RefundStatus status
    ) {
        return ApiResponse.ok(adminService.refunds(status));
    }

    @PostMapping("/refunds/{refundId}/decision")
    public ApiResponse<Void> decideRefund(
        @PathVariable Long refundId,
        @RequestBody RefundDecision request
    ) {
        adminService.decideRefund(refundId, request.approve());
        return ApiResponse.ok("已处理", null);
    }

    @GetMapping("/merchants")
    public ApiResponse<List<Map<String, Object>>> merchants() {
        return ApiResponse.ok(adminService.merchants());
    }

    @PatchMapping("/merchants/{merchantId}/status")
    public ApiResponse<Map<String, Object>> merchantStatus(
        @PathVariable Long merchantId,
        @RequestBody OpenRequest request
    ) {
        return ApiResponse.ok(adminService.toggleMerchant(merchantId, request.open()));
    }

    public record AssignRequest(Long riderId) {
    }

    public record RefundDecision(boolean approve) {
    }

    public record OpenRequest(boolean open) {
    }
}
