package com.shike.delivery.controller;

import com.shike.delivery.common.ApiResponse;
import com.shike.delivery.domain.DomainEnums.DeliveryStatus;
import com.shike.delivery.security.SecurityUtils;
import com.shike.delivery.service.RiderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rider")
@PreAuthorize("hasRole('RIDER')")
public class RiderController {

    private final RiderService riderService;

    public RiderController(RiderService riderService) {
        this.riderService = riderService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(riderService.dashboard(SecurityUtils.accountId()));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<Map<String, Object>>> tasks(
        @RequestParam(required = false) DeliveryStatus status
    ) {
        return ApiResponse.ok(riderService.tasks(SecurityUtils.accountId(), status));
    }

    @PostMapping("/tasks/{taskId}/pickup")
    public ApiResponse<Map<String, Object>> pickup(@PathVariable Long taskId) {
        return ApiResponse.ok(riderService.pickup(SecurityUtils.accountId(), taskId));
    }

    @PostMapping("/tasks/{taskId}/deliver")
    public ApiResponse<Map<String, Object>> deliver(@PathVariable Long taskId) {
        return ApiResponse.ok(riderService.deliver(SecurityUtils.accountId(), taskId));
    }
}
