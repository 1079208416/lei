package com.shike.delivery.auth;

import com.shike.delivery.common.ApiResponse;
import com.shike.delivery.domain.Account;
import com.shike.delivery.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.phone(), request.password()));
    }

    @PostMapping("/demo/{role}")
    public ApiResponse<Map<String, Object>> demoLogin(@PathVariable String role) {
        return ApiResponse.ok(authService.demoLogin(role));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        Account account = authService.requireAccount(SecurityUtils.accountId());
        return ApiResponse.ok(authService.me(account));
    }

    public record LoginRequest(
        @NotBlank(message = "不能为空") String phone,
        @NotBlank(message = "不能为空") String password
    ) {
    }
}
