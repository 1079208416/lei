package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shike.delivery.domain.DomainEnums.DeliveryStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("delivery_task")
public class DeliveryTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long riderId;
    private DeliveryStatus status;
    private LocalDateTime assignedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;
}
