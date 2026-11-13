package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product_option_group")
public class ProductOptionGroup {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String name;
    private Boolean requiredFlag;
    private Integer sortOrder;
}
