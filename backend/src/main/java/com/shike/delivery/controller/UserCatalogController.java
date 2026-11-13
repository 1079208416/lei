package com.shike.delivery.controller;

import com.shike.delivery.common.ApiResponse;
import com.shike.delivery.service.CatalogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('USER')")
public class UserCatalogController {

    private final CatalogService catalogService;

    public UserCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/merchants")
    public ApiResponse<List<Map<String, Object>>> merchants(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category
    ) {
        return ApiResponse.ok(catalogService.listMerchants(keyword, category));
    }

    @GetMapping("/merchants/{merchantId}")
    public ApiResponse<Map<String, Object>> merchant(@PathVariable Long merchantId) {
        return ApiResponse.ok(catalogService.merchantDetail(merchantId));
    }

    @GetMapping("/merchant-categories")
    public ApiResponse<List<String>> categories() {
        return ApiResponse.ok(catalogService.categories());
    }
}
