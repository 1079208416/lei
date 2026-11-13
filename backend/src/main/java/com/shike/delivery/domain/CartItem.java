package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cart_item")
public class CartItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long accountId;
    private Long merchantId;
    private Long productId;
    private Integer quantity;
    private String optionIds;
    private String optionsSnapshot;
    private Integer unitPrice;
    private String cartKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
