package com.shike.delivery.common;

public record ApiResponse<T>(String code, String message, T data, String requestId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("SUCCESS", "ok", data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }
}
