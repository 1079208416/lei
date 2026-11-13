package com.shike.delivery.domain;

public final class DomainEnums {

    private DomainEnums() {
    }

    public enum AccountRole {
        USER,
        MERCHANT,
        RIDER,
        ADMIN
    }

    public enum OrderStatus {
        WAITING_PAYMENT,
        WAITING_ACCEPT,
        PREPARING,
        WAITING_PICKUP,
        DELIVERING,
        COMPLETED,
        CANCELLED,
        REJECTED
    }

    public enum PaymentStatus {
        UNPAID,
        PAID,
        REFUNDING,
        REFUNDED,
        FAILED
    }

    public enum RefundStatus {
        NONE,
        REQUESTED,
        APPROVED,
        REJECTED,
        COMPLETED
    }

    public enum DeliveryStatus {
        ASSIGNED,
        PICKED_UP,
        DELIVERED,
        CANCELLED
    }
}
