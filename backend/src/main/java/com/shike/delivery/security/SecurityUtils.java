package com.shike.delivery.security;

import com.shike.delivery.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
        }
        return principal;
    }

    public static Long accountId() {
        return principal().id();
    }

    public static Long merchantId() {
        Long merchantId = principal().merchantId();
        if (merchantId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NO_MERCHANT", "当前账号未绑定商户");
        }
        return merchantId;
    }
}
