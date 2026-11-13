package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private String category;
    private String coverUrl;
    private String logoUrl;
    private String address;
    private Double longitude;
    private Double latitude;
    private BigDecimal rating;
    private Integer monthlySales;
    private Integer minOrderAmount;
    private Integer deliveryFee;
    private Integer packingFee;
    private Integer etaMinutes;
    private String notice;
    private Boolean open;
    private LocalDateTime createdAt;
}
