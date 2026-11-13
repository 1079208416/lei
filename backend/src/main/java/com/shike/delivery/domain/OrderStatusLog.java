package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shike.delivery.domain.DomainEnums.OrderStatus;
import com.shike.delivery.domain.DomainEnums.PaymentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_status_log")
public class OrderStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private String note;
    private String operatorRole;
    private Long operatorId;
    private LocalDateTime createdAt;
}
