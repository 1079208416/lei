package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product_option")
public class ProductOption {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private String name;
    private Integer priceDelta;
    private Integer sortOrder;
}
