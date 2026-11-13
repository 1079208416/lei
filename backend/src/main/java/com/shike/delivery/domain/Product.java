package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long merchantId;
    private Long categoryId;
    private String name;
    private String description;
    private String imageUrl;
    private Integer price;
    private Integer originalPrice;
    private Boolean soldOut;
    private Integer monthlySales;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
