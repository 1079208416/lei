package com.shike.delivery.security;

public record AuthPrincipal(Long id, String name, String role, Long merchantId) {
}
