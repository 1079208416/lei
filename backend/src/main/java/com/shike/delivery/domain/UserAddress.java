package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_address")
public class UserAddress {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private String label;
    private String contactName;
    private String phone;
    private String address;
    private String detail;
    private Double longitude;
    private Double latitude;
    @TableField("is_default")
    private Boolean defaultAddress;
    private LocalDateTime createdAt;
}
