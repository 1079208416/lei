package com.shike.delivery.controller;

import com.shike.delivery.common.ApiResponse;
import com.shike.delivery.domain.DomainEnums.OrderStatus;
import com.shike.delivery.security.SecurityUtils;
import com.shike.delivery.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/orders")
@PreAuthorize("hasRole('USER')")
public class UserOrderController {

    private final OrderService orderService;

    public UserOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody OrderService.CreateOrderRequest request) {
        return ApiResponse.ok(orderService.create(SecurityUtils.accountId(), request));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<Map<String, Object>> pay(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.pay(SecurityUtils.accountId(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Map<String, Object>> cancel(
        @PathVariable Long orderId,
        @RequestBody(required = false) ReasonRequest request
    ) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(orderService.cancel(SecurityUtils.accountId(), orderId, reason));
    }

    @PostMapping("/{orderId}/refund")
    public ApiResponse<Map<String, Object>> refund(
        @PathVariable Long orderId,
        @RequestBody ReasonRequest request
    ) {
        return ApiResponse.ok(
            orderService.requestRefund(SecurityUtils.accountId(), orderId, request.reason())
        );
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
        @RequestParam(required = false) OrderStatus status
    ) {
        return ApiResponse.ok(orderService.listForUser(SecurityUtils.accountId(), status));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long orderId) {
        return ApiResponse.ok(orderService.detail(orderId, SecurityUtils.accountId(), "USER"));
    }

    public record ReasonRequest(String reason) {
    }
}
