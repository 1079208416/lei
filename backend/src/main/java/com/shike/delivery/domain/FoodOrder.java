package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shike.delivery.domain.DomainEnums.OrderStatus;
import com.shike.delivery.domain.DomainEnums.PaymentStatus;
import com.shike.delivery.domain.DomainEnums.RefundStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("food_order")
public class FoodOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long accountId;
    private Long merchantId;
    private String merchantName;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private RefundStatus refundStatus;
    private String addressSnapshot;
    private String contactName;
    private String contactPhone;
    private Integer itemAmount;
    private Integer packingFee;
    private Integer deliveryFee;
    private Integer totalAmount;
    private String remark;
    private Long riderId;
    private String cancelReason;
    private LocalDateTime estimatedDeliveryAt;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime readyAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;
}
