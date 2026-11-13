package com.shike.delivery.controller;

import com.shike.delivery.common.ApiResponse;
import com.shike.delivery.domain.UserAddress;
import com.shike.delivery.security.SecurityUtils;
import com.shike.delivery.service.AddressService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@PreAuthorize("hasRole('USER')")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ApiResponse<List<UserAddress>> list() {
        return ApiResponse.ok(addressService.list(SecurityUtils.accountId()));
    }

    @PostMapping
    public ApiResponse<UserAddress> create(@RequestBody AddressService.AddressInput input) {
        return ApiResponse.ok(addressService.create(SecurityUtils.accountId(), input));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserAddress> update(
        @PathVariable Long id,
        @RequestBody AddressService.AddressInput input
    ) {
        return ApiResponse.ok(addressService.update(SecurityUtils.accountId(), id, input));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        addressService.delete(SecurityUtils.accountId(), id);
        return ApiResponse.ok("已删除", null);
    }
}
