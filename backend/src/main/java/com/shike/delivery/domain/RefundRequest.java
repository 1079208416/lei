package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shike.delivery.domain.DomainEnums.RefundStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("refund_request")
public class RefundRequest {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long accountId;
    private String reason;
    private Integer amount;
    private RefundStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
